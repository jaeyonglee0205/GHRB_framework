

if __name__ == '__main__':
    total = '''
@@ -59,7 +59,7 @@ public class ServerHttpAgentTest {
         Assert.assertNull(encode);
         Assert.assertEquals("namespace1", namespace);
         Assert.assertEquals("namespace1", tenant);
+        Assert.assertEquals("custom-aaa_8080_nacos_serverlist_namespace1", name);
         
     }

@@ -172,7 +172,7 @@ public class ClientWorkerTest {
         agent1.setAccessible(false);
         
         Assert.assertTrue(clientWorker.isHealthServer());
+        Assert.assertEquals(null, clientWorker.getAgentName());
     }
     
 }

@@ -36,7 +36,7 @@ public class ServerListManagerTest {
             Assert.fail();
         } catch (NacosException e) {
             Assert.assertEquals(
+                    "fail to get NACOS-server serverlist! env:custom-localhost_0_nacos_serverlist, not connnect url:http://localhost:0/nacos/serverlist",
                     e.getErrMsg());
         }
         mgr.shutdown();


    '''

    found = '''
    {
AndrolibResources androlibResources = new AndrolibResources();

Map<String, String> sdkInfo = new LinkedHashMap<>();
androlibResources.setSdkInfo(sdkInfo);
assertEquals("10000", androlibResources.checkTargetSdkVersionBounds());
    '''

    longest = '''
{
AndrolibResources androlibResources = new Andro
    '''

    total = total.replace("\n", "")
    found = found.replace("\n", "")
    longest = longest.replace("\n", "")

    total = len(total)
    found = len(found)
    longest = len(longest)

    found_total = round(found/total, 4)
    longest_total = round(longest/total, 4)
    
    print(found_total, longest_total)
    print(total)