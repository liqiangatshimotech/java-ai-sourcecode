import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.UUID;
/** 先启动 5.3 服务；只发送固定示例文本，动态 ID 由 UUID 生成。 */
public class PersistentMemorySmokeClient {
 public static void main(String[] args) throws Exception {
  String base=args.length==0?"http://127.0.0.1:8053":args[0];
  String id=UUID.randomUUID().toString();
  HttpClient client=HttpClient.newHttpClient();
  send(client,base,"a-"+id,"one","以后回答用中文，别太长。");
  send(client,base,"a-"+id,"two","请介绍会话记忆");
  send(client,base,"b-"+id,"two","请介绍会话记忆");
 }
 static void send(HttpClient client,String base,String user,String conversation,String message) throws Exception {
  String body="{\"userId\":\""+user+"\",\"conversationId\":\""+conversation+"\",\"message\":\""+message+"\"}";
  var request=HttpRequest.newBuilder(URI.create(base+"/api/persistent-memory-chat")).timeout(Duration.ofSeconds(120))
    .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
  var response=client.send(request,HttpResponse.BodyHandlers.ofString());
  if(response.statusCode()!=200)throw new IllegalStateException("HTTP "+response.statusCode());
  System.out.println(response.body());
 }
}
