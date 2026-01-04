package com.codelab.application;

import com.codelab.domain.repository.ExecutionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Docker代码执行服务测试
 * 
 * 运行前确保：
 * 1. Docker 已启动
 * 2. 沙箱镜像已构建：docker build -t c-codelab-sandbox:latest ./code-sandbox
 * 
 * 运行方式：
 * mvn test -Dtest=DockerCodeExecutionServiceTest -Ddocker.test.enabled=true
 * 或者直接运行这个测试类
 */
@SpringBootTest
@TestPropertySource(properties = {
    "code.tempDir=${java.io.tmpdir}/code-env-test",
    "code.sandboxImage=c-codelab-sandbox:latest",
    "code.dockerTimeout=15",
    "code.containerPool.size=3",
    "code.containerPool.maxIdleTime=300"
})
public class DockerCodeExecutionServiceTest {

    @MockBean
    private ExecutionRecordRepository recordRepository;

    @Autowired
    private DockerCodeExecutionService service;

    @Autowired
    private com.codelab.infrastructure.docker.ContainerPool containerPool;

    @BeforeEach
    void setUp() throws InterruptedException {
        // 如果是通过main方法直接运行测试，Spring注入不会生效，需要特殊处理
        if (containerPool == null) {
            System.out.println("警告: ContainerPool未被Spring注入，跳过容器池初始化检查");
            return;
        }
        
        // 等待容器池初始化完成，最多等待 15 秒
        System.out.println("等待容器池初始化...");
        long startTime = System.currentTimeMillis();
        while (containerPool.getStatus().getTotal() < 3 && (System.currentTimeMillis() - startTime) < 15000) {
            Thread.sleep(500);
        }
        System.out.println("容器池状态: " + containerPool.getStatus());
        assertTrue(containerPool.getStatus().getTotal() >= 3, "容器池应该至少有3个容器");
        assertTrue(containerPool.getStatus().getAvailable() >= 3, "容器池应该有3个可用容器");
    }

    @Test
    void testSimpleHelloWorld() {
        // 如果service为null，说明不是通过Spring运行的
        if (service == null) {
            System.out.println("跳过测试: DockerCodeExecutionService未被注入");
            return;
        }
        
        System.out.println("\n=== 测试 1: Hello World ===");
        String code = "#include <stdio.h>\n" +
                "int main() {\n" +
                "    printf(\"Hello, World!\\n\");\n" +
                "    return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = service.compileAndRun(code, null, null, "Test Hello World");

        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: " + result.getOutput());
        System.out.println("错误: " + result.getError());
        System.out.println("退出码: " + result.getExitCode());

        assertTrue(result.isSuccess(), "Hello World 应该执行成功");
        assertTrue(result.getOutput().contains("Hello, World"), "输出应该包含 Hello, World");
        assertEquals(0, result.getExitCode(), "退出码应该是 0");
    }

    @Test
    void testSimpleCalculation() {
        System.out.println("\n=== 测试 2: 简单计算 ===");
        String code = "#include <stdio.h>\n" +
                "int main() {\n" +
                "    int a = 10, b = 20;\n" +
                "    printf(\"Sum: %d\\n\", a + b);\n" +
                "    return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = service.compileAndRun(code, null, null, "Test Calculation");

        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: " + result.getOutput());
        System.out.println("错误: " + result.getError());
        System.out.println("退出码: " + result.getExitCode());

        assertTrue(result.isSuccess(), "计算程序应该执行成功");
        assertTrue(result.getOutput().contains("Sum: 30") || result.getOutput().contains("30"), 
                "输出应该包含计算结果");
    }

    @Test
    void testCompilationError() {
        System.out.println("\n=== 测试 3: 编译错误 ===");
        String code = "#include <stdio.h>\n" +
                "int main() {\n" +
                "    printf(\"Hello\\n\");\n" +
                "    return 0\n" +  // 缺少分号
                "}";

        DockerCodeExecutionService.ExecutionResult result = service.compileAndRun(code, null, null, "Test Compilation Error");

        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: " + result.getOutput());
        System.out.println("错误: " + result.getError());
        System.out.println("退出码: " + result.getExitCode());

        assertFalse(result.isSuccess(), "编译错误应该导致执行失败");
        assertTrue(result.getOutput().contains("error") || result.getOutput().contains("Error"), 
                "输出应该包含错误信息");
    }

    @Test
    void testRuntimeError() {
        System.out.println("\n=== 测试 4: 运行时错误 ===");
        String code = "#include <stdio.h>\n" +
                "int main() {\n" +
                "    int *p = NULL;\n" +
                "    *p = 10;  // 空指针解引用\n" +
                "    return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = service.compileAndRun(code, null, null, "Test Runtime Error");

        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: " + result.getOutput());
        System.out.println("错误: " + result.getError());
        System.out.println("退出码: " + result.getExitCode());

        // 运行时错误应该导致执行失败（非零退出码或执行失败）
        assertFalse(result.isSuccess() && result.getExitCode() == 0, 
                "运行时错误应该导致执行失败或非零退出码");
    }

    @Test
    void testCodeLengthLimit() {
        System.out.println("\n=== 测试 5: 代码长度限制 ===");
        // 创建超过 10KB 的代码
        StringBuilder longCode = new StringBuilder("#include <stdio.h>\nint main() { return 0; }\n");
        while (longCode.length() < 10 * 1024 + 100) {
            longCode.append("// 注释行\n");
        }

        DockerCodeExecutionService.ExecutionResult result = service.compileAndRun(longCode.toString(), null, null, "Test Long Code");

        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: " + result.getOutput());
        System.out.println("错误: " + result.getError());

        assertFalse(result.isSuccess(), "超过长度限制的代码应该被拒绝");
        assertTrue(result.getError().contains("长度超过限制"), "应该返回长度限制错误");
    }

    /**
     * 手动测试方法 - 可以直接运行来调试
     */
    public static void main(String[] args) {
        System.setProperty("docker.test.enabled", "true");
        
        // 创建测试实例
        DockerCodeExecutionServiceTest test = new DockerCodeExecutionServiceTest();
        
        try {
            test.setUp();
            
            // 检查Spring注入是否生效
            if (test.service == null) {
                System.err.println("错误: DockerCodeExecutionService未被Spring注入，请通过JUnit运行测试");
                return;
            }
            
            if (test.containerPool == null) {
                System.out.println("警告: ContainerPool未被Spring注入，将跳过容器池相关检查");
            }
            
            // 运行所有测试
            System.out.println("开始运行 Docker 代码执行测试...\n");
            
            test.testSimpleHelloWorld();
            Thread.sleep(1000);
            
            test.testSimpleCalculation();
            Thread.sleep(1000);
            
            test.testCompilationError();
            Thread.sleep(1000);
            
            test.testRuntimeError();
            Thread.sleep(1000);
            
            test.testCodeLengthLimit();
            
            System.out.println("\n所有测试完成！");
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

