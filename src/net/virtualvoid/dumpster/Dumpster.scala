package net.virtualvoid.dumpster

import scala.xml.{Node,Elem}

trait Resource {
  def property(prop:Node):Option[Node] = prop match {
  case t @ <resourcetype/> => Some(t)
  case _ => None
  }
  def url:String
  def children:Seq[Resource]
}

class FileResource(file:java.io.File) extends Resource{
  val formatter = {
    val df = new java.text.SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z")
    df.setTimeZone(java.util.TimeZone.getTimeZone("GMT"))
    df
  }
  def httpDate(time:long):String = formatter.format(new java.util.Date(time))
  
  override def property(prop:Node):Option[Node] = {
    def easyNode(value:Node):Option[Node] =
      prop match {case Elem(p,l,at,sc) => Some(Elem(p,l,at,sc,value))}
    def easy(value:String):Option[Node] =
      easyNode(scala.xml.Text(value))
    
    prop match{
    case <getlastmodified/> => easy(httpDate(file.lastModified))
    case <getcontentlength/> => easy(file.length.toString)
    case <resourcetype/> => {
      if (file.isDirectory) easyNode(<D:collection/>) else Some(prop)
    }
    case _ => super.property(prop)
    }
  }
  def url = "http://localhost:7070"+file.getPath.substring(1)+(if (file.isDirectory) "/" else "")
  def children = file.listFiles.map(new FileResource(_))
}

class MyHandler extends org.mortbay.jetty.handler.AbstractHandler{
  import javax.servlet.http._
  import org.mortbay.jetty.Request
  import scala.xml._
  
  def propfind(props:NodeSeq,target:String,depth:String) = {
    val file = new java.io.File("." + target)
    //System.out.println(file.getAbsolutePath+" "+file.isDirectory)
    val res:Resource = new FileResource(file)

    val resources:Seq[Resource] = depth match {
    case "0" => res::Nil
    case "1" => res.children ++ (res :: Nil)
    }
    
    <D:multistatus xmlns:D="DAV:">
      {resources.map(res =>{
        val mapped:Seq[(Node,Option[Node])] = props.map(p => (p,res.property(p)))
      <D:response>
        <D:href>{res.url}</D:href>
        <D:propstat xmlns:D="DAV:">
          <D:prop>
            {mapped.flatMap(_ match {case (_,Some(p))=>p::Nil;case (_,None)=>Nil})}
	      </D:prop>
	      <D:status>HTTP/1.1 200 OK</D:status>
        </D:propstat>
        <D:propstat xmlns:D="DAV:">
          <D:prop>
            {mapped.flatMap(_ match {case (_,Some(p))=>Nil;case (p,None)=>p})}
	      </D:prop>
	      <D:status>HTTP/1.1 404 Not Found</D:status>
        </D:propstat>
      </D:response>})}
    </D:multistatus>
  }
  
  def handle(target:String,req:HttpServletRequest,res:HttpServletResponse,dispatch:int){
    val r:Request = req.asInstanceOf[Request]
    r.setHandled(true)
    req.getMethod match {
    case "OPTIONS" => {
      res.setHeader("DAV","1")
      res.setHeader("Allow","GET,OPTIONS,PROPFIND")
    }
    case "PROPFIND" => {
      res.setContentType("application/xml")
      res.setStatus(207,"Multi-Status")
      val depth = req.getHeader("Depth")
      
      import scala.xml.XML
      
      val input = XML.load(req.getInputStream)
      
      XML.write(res.getWriter,propfind(input \ "prop" \ "_" ,target,depth),"utf-8",true,null)
    }
    case _ =>
      r.setHandled(false)
    }
  }
}

object Dumpster {
  def main(args:Array[String]){
    import org.mortbay.jetty.{Server,Handler}
    val s = new Server(7070)
    s.setHandler(new MyHandler)
    s.start
  }
}
