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
 * 安全测试类 - 基于安全测试报告的完整测试用例
 * 
 * 运行前确保：
 * 1. Docker 已启动
 * 2. 沙箱镜像已重新构建（包含安全修复）：
 *    docker build -t c-codelab-sandbox:latest ./code-sandbox
 * 3. 重启后端服务以应用新的容器配置
 * 
 * 运行方式：
 * mvn test -Dtest=SecurityTest
 * 或者直接在IDE中运行这个测试类
 */
@SpringBootTest
@TestPropertySource(properties = {
    "code.tempDir=${java.io.tmpdir}/code-env-test",
    "code.sandboxImage=c-codelab-sandbox:latest",
    "code.dockerTimeout=15",
    "code.containerPool.size=3",
    "code.containerPool.maxIdleTime=300"
})
public class SecurityTest {

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
        while (containerPool.getStatus().getTotal() < 3 && (System.currentTimeMillis() - startTime) < 20000) {
            Thread.sleep(500);
        }
        System.out.println("容器池状态: " + containerPool.getStatus());
    }

    // ==================== 安全测试用例 ====================

    @Test
    @DisplayName("SI-001: CPU耗尽攻击")
    void testSI001_CPUExhaustion() {
        String code = "#include <stdio.h>\n" +
                "\n" +
                "int main() {\n" +
                "    while(1) {}\n" +
                "    return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "SI-001");

        System.out.println("=== SI-001 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("退出码: " + result.getExitCode());

        // 应该被超时终止（非零退出码或执行失败）
        assertFalse(result.isSuccess() && result.getExitCode() == 0, 
                "CPU耗尽攻击应该被超时终止");
        assertTrue(result.getOutput().contains("超时") || 
                   result.getExitCode() != 0, 
                "应该检测到超时或非零退出码");
    }

    @Test
    @DisplayName("SI-002: 栈溢出攻击")
    void testSI002_StackOverflow() {
        String code = "#include <stdio.h>\n" +
                "\n" +
                "void recurse() {\n" +
                "    char buf[1024];\n" +
                "    recurse();\n" +
                "}\n" +
                "\n" +
                "int main() {\n" +
                "    recurse();\n" +
                "    return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "SI-002");

        System.out.println("=== SI-002 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("退出码: " + result.getExitCode());

        // 栈溢出应该导致段错误（非零退出码或执行失败）
        assertFalse(result.isSuccess() && result.getExitCode() == 0, 
                "栈溢出应该导致程序终止");
    }

    @Test
    @DisplayName("SI-003: fork炸弹攻击")
    void testSI003_ForkBomb() {
        String code = "#include <stdio.h>\n" +
                "#include <unistd.h>\n" +
                "\n" +
                "int main() {\n" +
                "    while(1) {\n" +
                "        fork();\n" +
                "    }\n" +
                "    return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "SI-003");

        System.out.println("=== SI-003 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("退出码: " + result.getExitCode());

        // fork应该被seccomp阻止，或者被pids-limit限制
        // 如果seccomp生效，fork会返回错误；如果pids-limit生效，会被终止
        assertFalse(result.isSuccess() && result.getExitCode() == 0, 
                "fork炸弹应该被阻止或限制");
    }

    @Test
    @DisplayName("SI-004: fork系统调用")
    void testSI004_ForkSystemCall() {
        String code = "#include <stdio.h>\n" +
                "#include <unistd.h>\n" +
                "#include <sys/wait.h>\n" +
                "\n" +
                "int main() {\n" +
                "    pid_t pid = fork();\n" +
                "    if (pid == 0) {\n" +
                "        printf(\"Child process\\n\");\n" +
                "    } else if (pid > 0) {\n" +
                "        wait(NULL);\n" +
                "        printf(\"Parent process\\n\");\n" +
                "    } else {\n" +
                "        printf(\"Fork failed: %d\\n\", pid);\n" +
                "    }\n" +
                "    return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "SI-004");

        System.out.println("=== SI-004 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("退出码: " + result.getExitCode());

        // fork应该被seccomp阻止
        assertTrue(result.getOutput().contains("Fork failed") || 
                   result.getExitCode() != 0, 
                "fork系统调用应该被seccomp阻止");
    }

    @Test
    @DisplayName("SI-005: system命令执行")
    void testSI005_SystemCommand() {
        String code = "#include <stdio.h>\n" +
                "#include <stdlib.h>\n" +
                "\n" +
                "int main() {\n" +
                "    int ret = system(\"echo 'test'\");\n" +
                "    printf(\"system() returned: %d\\n\", ret);\n" +
                "    return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "SI-005");

        System.out.println("=== SI-005 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("退出码: " + result.getExitCode());

        // system()内部会调用fork和execve，应该被阻止
        // 如果system()失败，返回码不为0
        // 注意：system()可能因为fork被阻止而失败
        if (result.isSuccess()) {
            // 如果执行成功，检查输出是否包含命令执行结果
            // 理想情况下，system()应该失败
            System.out.println("警告: system()可能执行成功，需要进一步检查");
        }
    }

    @Test
    @DisplayName("SI-006: setuid权限提升")
    void testSI006_SetuidPrivilegeEscalation() {
        String code = "#include <stdio.h>\n" +
                "#include <unistd.h>\n" +
                "#include <errno.h>\n" +
                "\n" +
                "int main() {\n" +
                "    int ret = setuid(0);\n" +
                "    if (ret == 0) {\n" +
                "        printf(\"setuid(0) succeeded!\\n\");\n" +
                "    } else {\n" +
                "        printf(\"setuid(0) failed: %d (errno=%d)\\n\", ret, errno);\n" +
                "    }\n" +
                "    return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "SI-006");

        System.out.println("=== SI-006 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("退出码: " + result.getExitCode());

        // setuid应该被seccomp阻止或权限不足
        assertTrue(result.getOutput().contains("failed") || 
                   result.getOutput().contains("errno"), 
                "setuid(0)应该失败（被seccomp阻止或权限不足）");
        assertFalse(result.getOutput().contains("succeeded"), 
                "setuid(0)不应该成功");
    }

    @Test
    @DisplayName("SI-007: 读取敏感文件")
    void testSI007_ReadSensitiveFile() {
        String code = "#include <stdio.h>\n" +
                "\n" +
                "int main() {\n" +
                "    FILE *fp = fopen(\"/etc/passwd\", \"r\");\n" +
                "    if (fp == NULL) {\n" +
                "        printf(\"Failed to open /etc/passwd\\n\");\n" +
                "        return 1;\n" +
                "    }\n" +
                "    char buf[1024];\n" +
                "    while (fgets(buf, sizeof(buf), fp)) {\n" +
                "        printf(\"%s\", buf);\n" +
                "    }\n" +
                "    fclose(fp);\n" +
                "    return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "SI-007");

        System.out.println("=== SI-007 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("退出码: " + result.getExitCode());

        // 由于使用非特权用户和只读文件系统，应该无法读取/etc/passwd
        // 或者文件系统隔离应该阻止访问
        assertTrue(result.getOutput().contains("Failed") || 
                   result.getExitCode() != 0, 
                "应该无法读取/etc/passwd（权限不足或文件系统隔离）");
    }

    @Test
    @DisplayName("SI-008: 写入系统目录")
    void testSI008_WriteSystemDirectory() {
        String code = "#include <stdio.h>\n" +
                "\n" +
                "int main() {\n" +
                "    FILE *fp = fopen(\"/etc/test.txt\", \"w\");\n" +
                "    if (fp == NULL) {\n" +
                "        printf(\"Failed to write /etc/test.txt\\n\");\n" +
                "        return 1;\n" +
                "    }\n" +
                "    fprintf(fp, \"test\\n\");\n" +
                "    fclose(fp);\n" +
                "    printf(\"Successfully wrote to /etc/test.txt\\n\");\n" +
                "    return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "SI-008");

        System.out.println("=== SI-008 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("退出码: " + result.getExitCode());

        // 由于只读文件系统和非特权用户，应该无法写入/etc
        assertTrue(result.getOutput().contains("Failed") || 
                   result.getExitCode() != 0, 
                "应该无法写入/etc目录（只读文件系统或权限不足）");
        assertFalse(result.getOutput().contains("Successfully"), 
                "不应该成功写入系统目录");
    }

    @Test
    @DisplayName("SI-009: 符号链接逃逸")
    void testSI009_SymlinkEscape() {
        String code = "#include <stdio.h>\n" +
                "#include <unistd.h>\n" +
                "\n" +
                "int main() {\n" +
                "    // 尝试创建符号链接\n" +
                "    int ret = symlink(\"/etc/passwd\", \"/app/code/link\");\n" +
                "    if (ret == 0) {\n" +
                "        printf(\"Symlink created\\n\");\n" +
                "        FILE *fp = fopen(\"/app/code/link\", \"r\");\n" +
                "        if (fp) {\n" +
                "            printf(\"Symlink read successful\\n\");\n" +
                "            fclose(fp);\n" +
                "        } else {\n" +
                "            printf(\"Symlink read failed\\n\");\n" +
                "        }\n" +
                "    } else {\n" +
                "        printf(\"Symlink creation failed\\n\");\n" +
                "    }\n" +
                "    return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "SI-009");

        System.out.println("=== SI-009 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("退出码: " + result.getExitCode());

        // 符号链接可能可以创建，但读取应该被文件系统隔离阻止
        // 或者由于权限问题无法读取目标文件
        if (result.getOutput().contains("Symlink created")) {
            assertTrue(result.getOutput().contains("read failed") || 
                       !result.getOutput().contains("read successful"), 
                    "即使创建了符号链接，读取也应该失败");
        }
    }

    @Test
    @DisplayName("SI-010: 内存耗尽攻击")
    void testSI010_MemoryExhaustion() {
        String code = "#include <stdio.h>\n" +
                "#include <stdlib.h>\n" +
                "\n" +
                "int main() {\n" +
                "    // 尝试分配大量内存（超过128MB限制）\n" +
                "    void *ptr = malloc(200 * 1024 * 1024);\n" +
                "    if (ptr == NULL) {\n" +
                "        printf(\"Memory allocation failed\\n\");\n" +
                "        return 1;\n" +
                "    }\n" +
                "    printf(\"Memory allocated successfully\\n\");\n" +
                "    free(ptr);\n" +
                "    return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "SI-010");

        System.out.println("=== SI-010 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("退出码: " + result.getExitCode());

        // 内存分配应该失败或被OOM Killer终止
        assertTrue(result.getOutput().contains("failed") || 
                   result.getExitCode() != 0, 
                "内存分配应该失败或被OOM Killer终止");
    }

    @Test
    @DisplayName("SI-011: 内存泄漏攻击")
    void testSI011_MemoryLeak() {
        String code = "#include <stdio.h>\n" +
                "#include <stdlib.h>\n" +
                "\n" +
                "int main() {\n" +
                "    // 持续分配内存但不释放\n" +
                "    for (int i = 0; i < 1000; i++) {\n" +
                "        void *ptr = malloc(1024 * 1024); // 1MB each\n" +
                "        if (ptr == NULL) {\n" +
                "            printf(\"Allocation failed at iteration %d\\n\", i);\n" +
                "            break;\n" +
                "        }\n" +
                "    }\n" +
                "    printf(\"Memory leak test completed\\n\");\n" +
                "    return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "SI-011");

        System.out.println("=== SI-011 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("退出码: " + result.getExitCode());

        // 由于内存限制（128MB），应该被OOM Killer终止或分配失败
        assertTrue(result.getOutput().contains("failed") || 
                   result.getExitCode() != 0 || 
                   !result.isSuccess(), 
                "内存泄漏应该被内存限制阻止");
    }

    @Test
    @DisplayName("SI-012: 多线程资源耗尽")
    void testSI012_MultiThreadResourceExhaustion() {
        String code = "#include <stdio.h>\n" +
                "#include <stdlib.h>\n" +
                "#include <pthread.h>\n" +
                "\n" +
                "void* thread_func(void* arg) {\n" +
                "    void *ptr = malloc(10 * 1024 * 1024); // 10MB\n" +
                "    while(1) { sleep(1); } // 保持线程运行\n" +
                "    free(ptr);\n" +
                "    return NULL;\n" +
                "}\n" +
                "\n" +
                "int main() {\n" +
                "    pthread_t threads[20];\n" +
                "    for (int i = 0; i < 20; i++) {\n" +
                "        pthread_create(&threads[i], NULL, thread_func, NULL);\n" +
                "    }\n" +
                "    sleep(5);\n" +
                "    return 0;\n" +
                "}";

        DockerCodeExecutionService.ExecutionResult result = 
            service.compileAndRun(code, null, null, "SI-012");

        System.out.println("=== SI-012 测试结果 ===");
        System.out.println("成功: " + result.isSuccess());
        System.out.println("输出: [" + result.getOutput() + "]");
        System.out.println("退出码: " + result.getExitCode());

        // 多线程内存分配应该被内存限制阻止
        assertTrue(result.getOutput().contains("failed") || 
                   result.getExitCode() != 0 || 
                   !result.isSuccess(), 
                "多线程资源耗尽应该被内存限制阻止");
    }
}

