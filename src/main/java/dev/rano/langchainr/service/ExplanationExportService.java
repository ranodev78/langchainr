package dev.rano.langchainr.service;

import dev.rano.langchainr.service.rag.ExplanationEmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ExplanationExportService {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter EXPLANATION_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a");

    private static final String MERMAID_SECTION_TEMPLATE = """
                  <div class="mermaid-section">
                      <h2>📊 Diagram</h2>
                      <div class="mermaid">%s</div>
                  </div>
                  """;

    private static final String EXPLANATION_TEMPLATE = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8"/>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                    <title>%s</title>
                    <link rel="stylesheet"
                          href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/vs2015.min.css"/>
                    <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
                    <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
                    <style>
                        * { box-sizing: border-box; margin: 0; padding: 0; }
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                            background: #1a1a2e;
                            color: #e0e0e0;
                            max-width: 900px;
                            margin: 0 auto;
                            padding: 2rem;
                        }
                        h1 { color: #00d4ff; margin-bottom: 1.5rem; font-size: 1.6rem; }
                        .meta { color: #888; font-size: 0.85rem; margin-bottom: 2rem; }
                        .content h1, .content h2, .content h3 { color: #00d4ff; margin: 1rem 0 0.5rem; }
                        .content p  { margin-bottom: 0.8rem; line-height: 1.6; }
                        .content ul, .content ol { padding-left: 1.5rem; margin-bottom: 0.8rem; }
                        .content li { margin-bottom: 0.3rem; line-height: 1.5; }
                        .content code {
                            background: #0f3460; padding: 0.15rem 0.4rem;
                            border-radius: 4px; font-family: 'Courier New', monospace;
                            font-size: 0.9em; color: #ffd166;
                        }
                        .content pre {
                            position: relative; padding-top: 2.8rem;
                            background: #0f3460; border-radius: 6px;
                            overflow-x: auto; margin: 0.8rem 0;
                        }
                        .content pre code { background: none; padding: 0 1rem 1rem; color: #e0e0e0; }
                        .copy-btn {
                            position: absolute; top: 0.5rem; right: 0.5rem;
                            padding: 0.25rem 0.7rem; font-size: 0.75rem;
                            background: #2d2d2d; color: #ccc;
                            border: 1px solid #555; border-radius: 4px;
                            cursor: pointer; opacity: 0;
                            transition: opacity 0.2s, background 0.2s;
                        }
                        .content pre:hover .copy-btn { opacity: 1; }
                        .copy-btn:hover { background: #3a3a3a; color: #fff; }
                        .copy-btn.copied { color: #4ec9b0; border-color: #4ec9b0; }
                        .mermaid-section { margin-top: 1.5rem; }
                        .mermaid-section h2 {
                            color: #00d4ff; margin-bottom: 1rem;
                            font-size: 1.1rem; text-transform: uppercase;
                        }
                        .mermaid { background: #0f3460; padding: 1rem; border-radius: 6px; text-align: center; }
                    </style>
                </head>
                <body>
                    <h1>%s</h1>
                    <p class="meta">Saved on %s</p>
                    <div class="content" id="content"></div>
                    %s
                    <script>
                        const raw = %s;
                        document.getElementById('content').innerHTML = marked.parse(raw);

                        document.querySelectorAll('.content pre code').forEach(b => hljs.highlightElement(b));

                        document.querySelectorAll('.content pre').forEach(pre => {
                            const btn = document.createElement('button');
                            btn.className = 'copy-btn';
                            btn.textContent = 'Copy';
                            btn.addEventListener('click', () => {
                                const code = pre.querySelector('code');
                                navigator.clipboard.writeText(code ? code.innerText : pre.innerText)
                                    .then(() => {
                                        btn.textContent = 'Copied!';
                                        btn.classList.add('copied');
                                        setTimeout(() => { btn.textContent = 'Copy'; btn.classList.remove('copied'); }, 2000);
                                    });
                            });
                            pre.appendChild(btn);
                        });

                        mermaid.initialize({ startOnLoad: true, theme: 'dark' });
                    </script>
                </body>
                </html>
                """;

    private final Path exportDir;
    private final ExplanationEmbeddingService embeddingService;

    public ExplanationExportService(@Value("${tutor.export.dir:${user.home}/Downloads}") final String exportDir,
                                    final ExplanationEmbeddingService embeddingService) {
        this.exportDir = Paths.get(exportDir);
        this.embeddingService = embeddingService;
    }

    public Path save(final String title, final String markdownContent, final String mermaidDiagram) throws IOException {
        Files.createDirectories(this.exportDir);

        final String safeTitle = title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")   // replace non-alphanumeric with dash
                .replaceAll("^-|-$", "") // trim leading/trailing dashes
                .substring(0, Math.min(title.length(), 60));

        final String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        final String filename = safeTitle + "_" + timestamp + ".html";
        final Path filePath = this.exportDir.resolve(filename);

        Files.writeString(filePath, buildHtml(title, markdownContent, mermaidDiagram));

        this.embeddingService.embed(title, markdownContent);

        return filePath;
    }

    private static String buildHtml(final String title, final String markdownContent, final String mermaidDiagram) {
        final String mermaidSection = mermaidDiagram == null ? "" : MERMAID_SECTION_TEMPLATE.formatted(mermaidDiagram);

        return EXPLANATION_TEMPLATE.formatted(
                escapeHtml(title),            // %s 1 → <title> tag
                escapeHtml(title),            // %s 2 → <h1> body heading
                LocalDateTime.now().format(EXPLANATION_TIMESTAMP_FORMAT), // %s 3 → .meta date
                mermaidSection,               // %s 4 → diagram block (or empty string)
                toJsonString(markdownContent)); // %s 5 → JS raw markdown
    }

    // Minimal HTML escaping for injected title/mermaid content
    private static String escapeHtml(final String input) {
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // Safely embed Markdown string inside a JS assignment
    private static String toJsonString(final String input) {
        return "`" + input.replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("$", "\\$") + "`";
    }
}
