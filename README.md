### LangChainR
---

- A Spring Boot application intended to provide a LeetCode problem solver assistant by explaining problems in great detail including:
   - Intuition
   - Dry Run
   - Similar problems
   - Prerequisites
   - Mermaid Chart Diagrams
- Incorporates RAG and chaining to ensure the solution provided is of the highest quality
- Use Claude API to prompt for solution to problems
- Features rich caching to avoid repeated API calls for already solved problems

## How to Run
1. If using Claude, you must have a Claude API key with credits (minimum $5) or use local LLM
2. Go to `http://localhost:8080/leetcode` and type in any LeetCode problem that you'd like to have the assistant explain the solution for
