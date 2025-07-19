package wiki.zhr.zouaiagent.tools;

import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;
import org.springframework.stereotype.Component;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Component
public class CodeCoverageAnalyzer {

    /**
     * 分析代码覆盖率
     *
     * @param sourceCode 源代码
     * @param testCode   测试代码
     * @return 覆盖率信息
     */
    public CoverageResult analyzeCoverage(String sourceCode, String testCode) throws Exception {
        // 创建临时目录
        Path tempDir = Files.createTempDirectory("coverage_analysis");

        try {
            // 编译源代码和测试代码
            Map<String, byte[]> compiledClasses = compileCode(sourceCode, testCode, tempDir);

            // 初始化JaCoCo运行时
            IRuntime runtime = new LoggerRuntime();
            RuntimeData data = new RuntimeData();
            runtime.startup(data);

            // 创建instrumented类
            Instrumenter instrumenter = new Instrumenter(runtime);
            Map<String, byte[]> instrumentedClasses = new HashMap<>();
            for (Map.Entry<String, byte[]> entry : compiledClasses.entrySet()) {
                instrumentedClasses.put(entry.getKey(),
                        instrumenter.instrument(entry.getValue(), entry.getKey()));
            }

            // 创建自定义类加载器并加载instrumented类
            MemoryClassLoader classLoader = new MemoryClassLoader(instrumentedClasses);

            // 执行测试
            ExecutionDataStore executionData = new ExecutionDataStore();
            SessionInfoStore sessionInfo = new SessionInfoStore();
            data.collect(executionData, sessionInfo, false);

            // 分析覆盖率
            CoverageBuilder coverageBuilder = new CoverageBuilder();
            Analyzer analyzer = new Analyzer(executionData, coverageBuilder);

            for (Map.Entry<String, byte[]> entry : compiledClasses.entrySet()) {
                analyzer.analyzeClass(entry.getValue(), entry.getKey());
            }

            // 计算覆盖率
            int coveredLines = 0;
            int totalLines = 0;
            int coveredBranches = 0;
            int totalBranches = 0;

            for (IClassCoverage classCoverage : coverageBuilder.getClasses()) {
                coveredLines += classCoverage.getLineCounter().getCoveredCount();
                totalLines += classCoverage.getLineCounter().getTotalCount();
                coveredBranches += classCoverage.getBranchCounter().getCoveredCount();
                totalBranches += classCoverage.getBranchCounter().getTotalCount();
            }

            return new CoverageResult(
                    calculatePercentage(coveredLines, totalLines),
                    calculatePercentage(coveredBranches, totalBranches));

        } finally {
            // 清理临时文件
            deleteDirectory(tempDir.toFile());
        }
    }

    private double calculatePercentage(int covered, int total) {
        return total == 0 ? 0.0 : (double) covered / total * 100;
    }

    private Map<String, byte[]> compileCode(String sourceCode, String testCode, Path tempDir) throws Exception {
        // 创建Java编译器
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        // 写入源代码和测试代码到临时文件
        Path sourceFile = tempDir.resolve("Source.java");
        Path testFile = tempDir.resolve("SourceTest.java");
        Files.write(sourceFile, sourceCode.getBytes());
        Files.write(testFile, testCode.getBytes());

        // 编译代码
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(
                Arrays.asList(sourceFile.toFile(), testFile.toFile()));
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null,
                compilationUnits);

        if (!task.call()) {
            throw new RuntimeException("Compilation failed: " + diagnostics.getDiagnostics());
        }

        // 读取编译后的类文件
        Map<String, byte[]> compiledClasses = new HashMap<>();
        Path classesDir = tempDir.resolve("classes");
        if (Files.exists(classesDir)) {
            Files.walk(classesDir)
                    .filter(path -> path.toString().endsWith(".class"))
                    .forEach(path -> {
                        try {
                            String className = path.toString()
                                    .substring(classesDir.toString().length() + 1)
                                    .replace(".class", "")
                                    .replace(File.separatorChar, '.');
                            compiledClasses.put(className, Files.readAllBytes(path));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        return compiledClasses;
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    /**
     * 自定义类加载器，用于加载instrumented类
     */
    private static class MemoryClassLoader extends ClassLoader {
        private final Map<String, byte[]> definitions;

        public MemoryClassLoader(Map<String, byte[]> definitions) {
            this.definitions = definitions;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = definitions.get(name);
            if (bytes != null) {
                return defineClass(name, bytes, 0, bytes.length);
            }
            return super.findClass(name);
        }
    }

    /**
     * 覆盖率结果类
     */
    public static class CoverageResult {
        private final double lineCoverage;
        private final double branchCoverage;

        public CoverageResult(double lineCoverage, double branchCoverage) {
            this.lineCoverage = lineCoverage;
            this.branchCoverage = branchCoverage;
        }

        public double getLineCoverage() {
            return lineCoverage;
        }

        public double getBranchCoverage() {
            return branchCoverage;
        }

        @Override
        public String toString() {
            return String.format("行覆盖率: %.2f%%, 分支覆盖率: %.2f%%", lineCoverage, branchCoverage);
        }
    }
}