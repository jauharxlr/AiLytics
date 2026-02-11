# R&D: Gemini Document Processing with Spring Boot

## Objective
Develop a Spring Boot application that processes PDFs and Images using Gemini AI and returns structured JSON output based on user-defined schemas.

## Model Version Verification (February 2026)
- **Status**: As of February 2026, **Gemini 2.0 Flash** is the most advanced stable multimodal model released by Google.
- **Gemini 2.5 Flash**: Currently not officially released or available in public SDKs. 
- **Future-Proofing**: The application uses Spring AI's `ChatClient` abstraction. Swapping to Gemini 2.5 Flash upon release will require only a single line change in `application.properties`:
  `spring.ai.google.gemini.chat.options.model=gemini-2.5-flash`

## Technical Stack
1.  **Framework**: Spring Boot 3.4+
2.  **AI Integration**: [Spring AI](https://spring.io/projects/spring-ai) with the Google Gemini module.
3.  **Model**: `gemini-2.0-flash` (Highest available stable version).
4.  **JSON Mapping**: Spring AI's `Structured Output` (using `BeanOutputConverter` or `ChatClient.entity()`).
5.  **File Handling**: `MultipartFile` for initial upload; Spring AI `Resource` or `Media` for model input.

## Gemini 2.0/2.5 Flash Capabilities
- **Enhanced Speed**: Optimized for low-latency responses.
- **Improved Multimodal Support**: Native support for high-resolution images and complex PDF structures.
- **Strict JSON Mode**: Native support for controlled JSON output via schema constraints.
- **Long Context**: Handling large documents with high precision.

## Core Components

### 1. Document Input (Multipart)
The application will expose a REST endpoint accepting `MultipartFile`.
- PDFs and Images (JPEG/PNG) are supported directly by Gemini 1.5.
- Inline processing for small files; Google Cloud Storage (GCS) or Gemini File API for larger documents.

### 2. Gemini Integration (Spring AI)
Spring AI provides a simplified way to interact with Gemini.
```java
var chatResponse = chatClient.prompt()
    .user(u -> u.text(userPrompt)
               .media(Media.builder()
                   .type(MimeTypeUtils.APPLICATION_PDF)
                   .data(fileResource)
                   .build()))
    .call()
    .entity(TargetJsonClass.class);
```

### 3. Structured Output (User-Defined JSON)
To return a specific JSON structure:
- **Approach A: Static Schema**: Define Java POJOs and use `BeanOutputConverter`.
- **Approach B: Dynamic Schema**: Pass a JSON schema in the prompt and receive a raw string or Map. Gemini supports "Response MIME Type" as `application/json` with a provided JSON schema for strict adherence.

### 4. Workflow
1. Client sends `POST /process` with `file` and `prompt`.
2. Controller receives `MultipartFile`.
3. Spring AI `ChatClient` constructs a multimodal prompt.
4. Gemini processes the document and extracts data.
5. Spring AI parses the output into the desired JSON structure.
6. Response returned to client.

## Challenges & Considerations
- **Token Limits**: PDFs can be large; Gemini 1.5 Pro's 2M context window handles most cases.
- **Data Privacy**: Ensure documents are handled securely and not stored permanently unless required.
- **Latency**: Multimodal processing takes time; consider asynchronous processing (WebSockets or Polling) for large files.
