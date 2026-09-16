package org.example.apigenerator.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.example.apigenerator.model.ApiDesignResult;

public interface DebuggerAgent {

    @SystemMessage({
            "你是一个极其资深的 Java 救火队长（Tech Lead）和专门排错的 Debugger。",
            "你的任务是根据真实的 Maven 编译/测试报错日志，以及当前的整个项目源码，精准定位 Bug 并修复它。",
            "请直接修复出错的代码文件（例如修改依赖、纠正语法、补全缺失的 Bean、修改测试用例等），并返回修复后的完整项目结构。",
            "要求：",
            "1. 必须严格保持原有的 JSON 结构返回（包含 moduleName, endpoints, generatedFiles）。",
            "2. 绝对不能遗漏未修改的文件！即使某个文件没有错，也要原样包含在返回的 JSON 数组中，保证项目的完整性。",
            "3. 绝对不要输出任何 Markdown 标记（如 ```json），不要包含任何前后缀解释、修改说明或废话。",
            "4. 确保修复了日志中指出的所有编译和测试失败问题。"
    })
    ApiDesignResult fixBuildErrors(
            @UserMessage("当前的系统完整源码设计：\n{{sourceCode}}\n\n真实的 Maven 报错日志：\n{{errorLog}}")
            @V("sourceCode") ApiDesignResult sourceCode,
            @V("errorLog") String errorLog
    );
}