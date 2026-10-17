import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
/** 本机认证客户端；清除的是当前 user-a 的全部记忆，请用于专门的演示账号。 */
public class MemoryServiceSmokeClient {
 public static void main(String[] args) throws Exception {
  String base=args.length==0?"http://127.0.0.1:8056":args[0];
  String password=System.getenv("MEMORY_USER_A_PASSWORD");
  if(password==null||password.isBlank())throw new IllegalStateException("请设置 MEMORY_USER_A_PASSWORD");
  String auth="Basic "+Base64.getEncoder().encodeToString(("user-a:"+password).getBytes(StandardCharsets.UTF_8));
  String conversation=UUID.randomUUID().toString();
  HttpClient client=HttpClient.newHttpClient();
  send(client,base+"/api/memory/"+conversation+"/chat","POST","{\"message\":\"以后回答用中文，尽量短一点。\"}",auth,200);
  send(client,base+"/api/memory/"+conversation,"GET","",auth,200);
  send(client,base+"/api/memory","DELETE","",auth,204);
  send(client,base+"/api/memory/"+conversation,"GET","",auth,200);
 }
 static void send(HttpClient client,String url,String method,String body,String auth,int expected) throws Exception {
  var request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(120))
    .header("Authorization",auth).header("Content-Type","application/json")
    .method(method,body.isEmpty()?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build();
  var response=client.send(request,HttpResponse.BodyHandlers.ofString());
  if(response.statusCode()!=expected)throw new IllegalStateException("HTTP "+response.statusCode());
  System.out.println(method+" "+response.statusCode()+" "+response.body());
 }
}
