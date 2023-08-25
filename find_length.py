

if __name__ == '__main__':
    total = '''

@@ -2900,6 +2900,28 @@ public class IndentationCheckTest extends AbstractModuleTestSupport {
                 expected);
     }
 
+    @Test
+    public void testIndentationLongConcatenatedString() throws Exception {
+        final DefaultConfiguration checkConfig = createModuleConfig(IndentationCheck.class);
+        checkConfig.addProperty("tabWidth", "4");
+
+        final String[] expected = CommonUtil.EMPTY_STRING_ARRAY;
+
+        verifyWarns(checkConfig, getPath("InputIndentationLongConcatenatedString.java"),
+                expected);
+    }
+
+    @Test
+    public void testIndentationLineBreakVariableDeclaration()
+            throws Exception {
+        final DefaultConfiguration checkConfig = createModuleConfig(IndentationCheck.class);
+        checkConfig.addProperty("tabWidth", "4");
+
+        final String fileName = getPath("InputIndentationLineBreakVariableDeclaration.java");
+        final String[] expected = CommonUtil.EMPTY_STRING_ARRAY;
+        verifyWarns(checkConfig, fileName, expected);
+    }
+
     private static final class IndentAudit implements AuditListener {
 
         private final IndentComment[] comments;

    '''

    found = '''
ublic class IndentationCheckTest extends AbstractM
final DefaultConfiguration checkConfig = createModuleConfig(IndentationCheck.class);
final String[] expected = CommonUtil.EMPTY_STRING_ARRAY;
final DefaultConfiguration checkConfig = createModuleConfig(IndentationCheck.class);
final String[] expected = CommonUtil.EMPTY_STRING_ARRAY;
    '''

    longest = '''
ublic class IndentationCheckTest extends AbstractM
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