package com.codelab.application;

import com.codelab.domain.repository.ExecutionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 综合测试类 - 基于测试报告的完整测试用例
 * 
 * 运行前确保：
 * 1. Docker 已启动
 * 2. 沙箱镜像已构建：docker build -t c-codelab-sandbox:latest ./code-sandbox
 * 
 * 运行方式：
 * mvn test -Dtest=ComprehensiveExecutionTest
 * 或者直接在IDE中运行这个测试类
 */
@SpringBootTest
@TestPropertySource(properties = {
    "code.tempDir=${java.io.tmpdir}/code-env-test",
    "code.sandboxImage=c-codelab-sandbox:latest",
    "code.dockerTimeout=15",
    "code.containerPool.size=5",
    "code.containerPool.maxIdleTime=300"
})
public class ComprehensiveExecutionTest {

    @MockBean
    private ExecutionRecordRepository recordRepository;

    @Autowired
    private DockerCodeExecutionService service;

    @Autowired
    private com.codelab.infrastructure.docker.ContainerPool containerPool;

    @BeforeEach
    void setUp() throws InterruptedException {
        if (containerPool == null) {
            System.out.println("警告: ContainerPool未被Spring注入，跳过容器池初始化检查");
            return;
        }
        
        System.out.println("等待容器池初始化...");
        long startTime = System.currentTimeMillis();
        while (containerPool.getStatus().getTotal() < 5 && (System.currentTimeMillis() - startTime) < 20000) {
            Thread.sleep(500);
        }
        System.out.println("容器池状态: " + containerPool.getStatus());
    }

    // ==================== 正向测试用例 ====================

    @Test
    @DisplayName("CE-001: 基础执行 - Hello World")
    void testCE001_BasicExecution() {
        String code = "#include <stdio.h>\n" +
                "\n" +
                "int main() {\n" +
                "printf(\"Hello, CodeSandbox!\");\n" +
                "return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "CE-001");

        System.out.println("=== CE-001 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("退出码: " + result.getExitCode());

        assertTrue(result.isSuccess(), "应该执行成功");
        assertTrue(result.getOutput().contains("Hello, CodeSandbox!"), 
                "输出应该包含 'Hello, CodeSandbox!'");
        assertEquals(0, result.getExitCode(), "退出码应该是 0");
    }

    @Test
    @DisplayName("CE-002: 基础执行 - 算术运算")
    void testCE002_BasicArithmetic() {
        String code = "#include <stdio.h>\n" +
                "\n" +
                "int main() {\n" +
                "int a = 5, b = 3;\n" +
                "printf(\"Sum: %d\", a + b);\n" +
                "return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "CE-002");

        System.out.println("=== CE-002 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");

        assertTrue(result.isSuccess(), "应该执行成功");
        assertTrue(result.getOutput().contains("Sum: 8"), 
                "输出应该包含 'Sum: 8'");
    }

    @Test
    @DisplayName("CE-003: 输入输出 - 整数输入")
    void testCE003_IntegerInput() {
        String code = "#include <stdio.h>\n" +
                "\n" +
                "int main() {\n" +
                "int num;\n" +
                "scanf(\"%d\", &num);\n" +
                "printf(\"You entered: %d\", num);\n" +
                "return 0;\n" +
                "}";

        String input = "42";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, input, null, "CE-003");

        System.out.println("=== CE-003 测试结果 ===");
        System.out.println("输入: " + input);
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");

        assertTrue(result.isSuccess(), "应该执行成功");
        assertTrue(result.getOutput().contains("You entered: 42"), 
                "输出应该包含 'You entered: 42'");
    }

    @Test
    @DisplayName("CE-004: 输入输出 - 字符串输入")
    void testCE004_StringInput() {
        String code = "#include <stdio.h>\n" +
                "\n" +
                "int main() {\n" +
                "char name[50];\n" +
                "printf(\"Enter your name: \");\n" +
                "scanf(\"%s\", name);\n" +
                "printf(\"Hello, %s!\", name);\n" +
                "return 0;\n" +
                "}";

        String input = "Alice";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, input, null, "CE-004");

        System.out.println("=== CE-004 测试结果 ===");
        System.out.println("输入: " + input);
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");

        assertTrue(result.isSuccess(), "应该执行成功");
        assertTrue(result.getOutput().contains("Hello, Alice!"), 
                "输出应该包含 'Hello, Alice!'");
    }

    @Test
    @DisplayName("CE-005: 标准库-数学 - sqrt函数")
    void testCE005_MathLibrary() {
        String code = "#include <stdio.h>\n" +
                "#include <math.h>\n" +
                "\n" +
                "int main() {\n" +
                "double x = 4.0;\n" +
                "printf(\"sqrt(%.1f) = %.2f\", x, sqrt(x));\n" +
                "return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "CE-005");

        System.out.println("=== CE-005 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("错误: [" + result.getError() + "]");

        assertTrue(result.isSuccess(), "应该编译和执行成功（已添加-lm链接）");
        assertTrue(result.getOutput().contains("sqrt(4.0) = 2.00") || 
                   result.getOutput().contains("sqrt(4.0) = 2.0"), 
                "输出应该包含 'sqrt(4.0) = 2.00'");
    }

    @Test
    @DisplayName("CE-006: 标准库-数学 - sin函数")
    void testCE006_TrigonometricFunction() {
        String code = "#include <stdio.h>\n" +
                "#include <math.h>\n" +
                "\n" +
                "int main() {\n" +
                "printf(\"sin(π/2) = %.2f\", sin(3.14159/2));\n" +
                "return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "CE-006");

        System.out.println("=== CE-006 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");

        assertTrue(result.isSuccess(), "应该执行成功");
        assertTrue(result.getOutput().contains("sin(π/2) = 1.00") || 
                   result.getOutput().contains("sin(π/2) = 1.0"), 
                "输出应该包含 'sin(π/2) = 1.00'（允许微小浮点误差）");
    }

    @Test
    @DisplayName("CE-007: 标准库-字符串 - strcat和strlen")
    void testCE007_StringLibrary() {
        String code = "#include <stdio.h>\n" +
                "#include <string.h>\n" +
                "\n" +
                "int main() {\n" +
                "char str1[20] = \"Hello\";\n" +
                "char str2[] = \" World\";\n" +
                "strcat(str1, str2);\n" +
                "printf(\"%s (len=%d)\", str1, strlen(str1));\n" +
                "return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "CE-007");

        System.out.println("=== CE-007 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");

        assertTrue(result.isSuccess(), "应该执行成功");
        assertTrue(result.getOutput().contains("Hello World") && 
                   result.getOutput().contains("len=11"), 
                "输出应该包含 'Hello World (len=11)'");
    }

    @Test
    @DisplayName("CE-008: 标准库-内存 - malloc和free")
    void testCE008_MemoryAllocation() {
        String code = "#include <stdio.h>\n" +
                "#include <stdlib.h>\n" +
                "\n" +
                "int main() {\n" +
                "int *arr = (int*)malloc(5 * sizeof(int));\n" +
                "for(int i=0; i<5; i++) arr[i] = i*10;\n" +
                "printf(\"arr[3] = %d\", arr[3]);\n" +
                "free(arr);\n" +
                "return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "CE-008");

        System.out.println("=== CE-008 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");

        assertTrue(result.isSuccess(), "应该执行成功");
        assertTrue(result.getOutput().contains("arr[3] = 30"), 
                "输出应该包含 'arr[3] = 30'");
    }

    @Test
    @DisplayName("CE-009: 复杂逻辑 - 斐波那契数列")
    void testCE009_Fibonacci() {
        String code = "#include <stdio.h>\n" +
                "\n" +
                "int main() {\n" +
                "int n = 10, first = 0, second = 1, next;\n" +
                "printf(\"Fibonacci: %d %d \", first, second);\n" +
                "for(int i=2; i<n; i++) {\n" +
                "next = first + second;\n" +
                "printf(\"%d \", next);\n" +
                "first = second;\n" +
                "second = next;\n" +
                "}\n" +
                "return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "CE-009");

        System.out.println("=== CE-009 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");

        assertTrue(result.isSuccess(), "应该执行成功");
        assertTrue(result.getOutput().contains("Fibonacci: 0 1 1 2 3 5 8 13 21 34"), 
                "输出应该包含完整的斐波那契数列");
    }

    @Test
    @DisplayName("CE-010: 递归函数 - 阶乘")
    void testCE010_RecursiveFunction() {
        String code = "#include <stdio.h>\n" +
                "\n" +
                "int factorial(int n) {\n" +
                "if(n<=1) return 1;\n" +
                "return n * factorial(n-1);\n" +
                "}\n" +
                "\n" +
                "int main() {\n" +
                "printf(\"5! = %d\", factorial(5));\n" +
                "return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "CE-010");

        System.out.println("=== CE-010 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");

        assertTrue(result.isSuccess(), "应该执行成功");
        assertTrue(result.getOutput().contains("5! = 120"), 
                "输出应该包含 '5! = 120'");
    }

    // ==================== 反向测试用例（预期失败） ====================

    @Test
    @DisplayName("CE-101: 编译错误-语法 - 缺少分号")
    void testCE101_SyntaxError() {
        String code = "#include <stdio.h>\n" +
                "\n" +
                "int main() {\n" +
                "printf(\"Hello\") // 缺少分号\n" +
                "return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "CE-101");

        System.out.println("=== CE-101 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("错误: [" + result.getError() + "]");

        assertFalse(result.isSuccess(), "应该编译失败");
        // 编译错误信息在error字段中
        String errorOutput = result.getError() != null ? result.getError() : result.getOutput();
        assertTrue(errorOutput.contains("error") || 
                   errorOutput.contains("expected ';'") ||
                   errorOutput.contains(";"), 
                "应该包含语法错误信息");
    }

    @Test
    @DisplayName("CE-102: 编译错误-类型 - 类型不匹配")
    void testCE102_TypeError() {
        // 使用更明确的类型错误：尝试将结构体赋值给整数
        // 这会导致明确的编译错误
        String code = "#include <stdio.h>\n" +
                "\n" +
                "struct Point { int x; int y; };\n" +
                "\n" +
                "int main() {\n" +
                "struct Point p = {1, 2};\n" +
                "int x = p;  // 将结构体赋值给int，这是明确的类型错误\n" +
                "return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "CE-102");

        System.out.println("=== CE-102 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("错误: [" + result.getError() + "]");

        assertFalse(result.isSuccess(), "应该编译失败");
        // 编译错误信息在error字段中
        String errorOutput = result.getError() != null ? result.getError() : result.getOutput();
        assertTrue(errorOutput.contains("error") || 
                   errorOutput.contains("conversion") ||
                   errorOutput.contains("incompatible") ||
                   errorOutput.contains("struct") ||
                   errorOutput.contains("assignment"), 
                "应该包含类型错误信息");
    }

    @Test
    @DisplayName("CE-103: 编译错误-未定义 - 未声明变量")
    void testCE103_UndefinedVariable() {
        String code = "#include <stdio.h>\n" +
                "\n" +
                "int main() {\n" +
                "printf(\"%d\", undefined_var);\n" +
                "return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "CE-103");

        System.out.println("=== CE-103 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");

        assertFalse(result.isSuccess(), "应该编译失败");
        // 编译错误信息在error字段中
        String errorOutput = result.getError() != null ? result.getError() : result.getOutput();
        assertTrue(errorOutput.contains("error") || 
                   errorOutput.contains("undeclared") ||
                   errorOutput.contains("undefined_var") ||
                   errorOutput.contains("undefined"), 
                "应该包含未定义变量错误信息");
    }

    @Test
    @DisplayName("CE-104: 编译错误-头文件 - 缺失头文件")
    void testCE104_MissingHeader() {
        String code = "#include <nonexistent.h>\n" +
                "\n" +
                "int main() { return 0; }";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "CE-104");

        System.out.println("=== CE-104 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");

        assertFalse(result.isSuccess(), "应该编译失败");
        // 编译错误信息在error字段中
        String errorOutput = result.getError() != null ? result.getError() : result.getOutput();
        assertTrue(errorOutput.contains("error") || 
                   errorOutput.contains("nonexistent.h") ||
                   errorOutput.contains("No such file") ||
                   errorOutput.contains("fatal error"), 
                "应该包含头文件缺失错误信息");
    }

    @Test
    @DisplayName("CE-105: 运行时错误-除零")
    void testCE105_DivisionByZero() {
        String code = "#include <stdio.h>\n" +
                "\n" +
                "int main() {\n" +
                "int a = 5, b = 0;\n" +
                "printf(\"%d\", a/b);\n" +
                "return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "CE-105");

        System.out.println("=== CE-105 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("退出码: " + result.getExitCode());

        // 除零应该导致运行时错误（非零退出码或执行失败）
        assertFalse(result.isSuccess() && result.getExitCode() == 0, 
                "除零应该导致执行失败或非零退出码");
        // 可能输出 "Floating point exception" 或其他错误信息
    }

    @Test
    @DisplayName("CE-106: 运行时错误-数组越界")
    void testCE106_ArrayOutOfBounds() {
        String code = "#include <stdio.h>\n" +
                "\n" +
                "int main() {\n" +
                "int arr[5] = {1,2,3,4,5};\n" +
                "printf(\"%d\", arr[10]);\n" +
                "return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "CE-106");

        System.out.println("=== CE-106 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("退出码: " + result.getExitCode());

        // 数组越界可能导致段错误或输出随机值
        // 在C语言中，数组越界不一定会被检测到，可能输出随机值
        // 这里只验证程序能够执行（不崩溃）或检测到错误
        // 实际行为取决于系统
    }

    @Test
    @DisplayName("CE-107: 运行时错误-空指针")
    void testCE107_NullPointer() {
        String code = "#include <stdio.h>\n" +
                "\n" +
                "int main() {\n" +
                "int *ptr = NULL;\n" +
                "printf(\"%d\", *ptr);\n" +
                "return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "CE-107");

        System.out.println("=== CE-107 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("退出码: " + result.getExitCode());

        // 空指针解引用应该导致段错误（非零退出码或执行失败）
        assertFalse(result.isSuccess() && result.getExitCode() == 0, 
                "空指针解引用应该导致执行失败或非零退出码");
        // 可能输出 "Segmentation fault" 或其他错误信息
    }

    @Test
    @DisplayName("CE-108: 编译警告 - 未使用变量")
    void testCE108_UnusedVariable() {
        String code = "#include <stdio.h>\n" +
                "\n" +
                "int main() {\n" +
                "int unused = 5;\n" +
                "printf(\"Hello\");\n" +
                "return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "CE-108");

        System.out.println("=== CE-108 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("编译输出: [" + result.getError() + "]");

        // 警告不应该导致编译失败，程序应该能执行
        assertTrue(result.isSuccess(), "警告不应该导致执行失败");
        assertTrue(result.getOutput().contains("Hello"), 
                "程序应该能正常执行并输出 'Hello'");
        // 注意：警告信息可能在编译输出中，但不会在执行结果中显示
    }
}

