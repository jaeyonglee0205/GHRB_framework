

if __name__ == '__main__':
    total = '''

@@ -101,7 +101,7 @@ public class InvalidSdkBoundingTest extends BaseTest {
         AndrolibResources androlibResources = new AndrolibResources();
 
         Map<String, String> sdkInfo = new LinkedHashMap<>();
+        sdkInfo.put("targetSdkVersion", "VANILLAICECREAM");
 
         androlibResources.setSdkInfo(sdkInfo);
         assertEquals("10000", androlibResources.checkTargetSdkVersionBounds());

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