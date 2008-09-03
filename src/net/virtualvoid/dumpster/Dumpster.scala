package net.virtualvoid.dumpster

class MyHandler extends org.mortbay.jetty.handler.AbstractHandler{
  import javax.servlet.http._
  import org.mortbay.jetty.Request
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
      scala.xml.XML.write(res.getWriter,
<D:multistatus xmlns:D="DAV:">
  <D:response>
    <D:href>http://localhost:7070/</D:href>
    <D:propstat>
      <D:prop>
        <D:resourcetype><D:collection/></D:resourcetype>
      </D:prop>
      <D:status>HTTP/1.1 200 OK</D:status>
    </D:propstat>
  </D:response>
  {if (depth == "1") (<D:response>
    <D:href>http://localhost:7070/wurst</D:href>
    <D:propstat>
    <D:prop>
      <D:resourcetype/>
    </D:prop>
    <D:status>HTTP/1.1 200 OK</D:status>
    </D:propstat>
  </D:response>
  <D:response>
    <D:href>http://localhost:7070/blub</D:href>
    <D:propstat>
      <D:prop>
        <D:resourcetype/>
      </D:prop>
      <D:status>HTTP/1.1 200 OK</D:status>
    </D:propstat>
  </D:response>)}
 </D:multistatus>,"utf-8",true,null)
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
