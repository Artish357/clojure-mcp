# Clojure MCP - REPL-Driven Development with AI Assistance

A Model Context Protocol (MCP) server for Clojure that enables AI assistants (like Claude) to interact directly with a Clojure REPL, providing a collaborative, REPL-driven development workflow between humans and LLMs.

## 🚀 Overview

This project implements an MCP server that connects AI models to a Clojure nREPL, enabling a powerful REPL-driven development workflow. The driving philosophy is:

> Tiny steps with high quality rich feedback is the recipe for the sauce.

The project enables LLMs to:
- Evaluate Clojure code and see immediate results
- Incrementally develop solutions with step-by-step verification
- Navigate and explore namespaces and symbols
- Edit Clojure files with proper formatting
- Look up documentation and source code
- Test code directly in the REPL environment

### Why REPL-Driven Development with AI?

This approach enables:
- **Immediate feedback** - Validate code by running it, not just statically analyzing it
- **Incremental development** - Build solutions in small, verified steps
- **Human oversight** - Keep the programmer in the loop for guidance
- **Functional approach** - Encourage pure functions that are easier to understand and test
- **Enhanced context** - The LLM learns from each step's result, improving future steps

## 📋 Installation

### Prerequisites

- [Clojure](https://clojure.org/guides/install_clojure) (1.11 or later)
- [Java](https://openjdk.org/) (JDK 11 or later)
- [Claude Desktop](https://claude.ai/desktop) (for the best experience)
- [MCP Server for Filesystem](https://github.com/anthropics/ModelContextProtocol) (`npx -y @modelcontextprotocol/server-filesystem`)
- [MCP Server for Git](https://github.com/Anthropic-Labs/mcp-server-git) (optional, for Git integration)

### Setting up the project

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/clojure-mcp.git
   cd clojure-mcp
   ```

2. Configure Claude Desktop to use the Clojure MCP server:

   Edit your Claude Desktop config file at `~/Library/Application Support/Claude/claude_desktop_config.json`:
   
   ```json
   {
       "mcpServers": {
           "filesystem": {
               "command": "/bin/sh",
               "args": [
                   "-c",
                   "PATH=/your/bin/path:$PATH exec npx -y @modelcontextprotocol/server-filesystem /path/to/your/workspace"
                ]
           },
           "git": {
               "command": "/bin/sh",
               "args": [
                   "-c",
                   "PATH=/path/to/python:$PATH exec uvx mcp-server-git --repository /path/to/your/workspace/clojure-mcp/.git"
               ]
           },
           "clojure_connect": {
               "command": "/bin/sh",
               "args": [
                   "-c",
                   "cd /path/to/your/workspace/clojure-mcp && PATH=/your/bin/path:$PATH && clojure -X:mcp"
                ]
           }
       }
   }
   ```
   
   Replace the paths with your specific configuration:
   - `/your/bin/path` - Path to your binaries (e.g., `/Users/username/.nix-profile/bin`)
   - `/path/to/your/workspace` - Path to your workspace directory
   - `/path/to/python` - Path to your Python installation if using Git integration

4. Launch Claude Desktop and start a new conversation.

> If you change the server you must restart Claude Desktop to see the changes.

### Integarted prompt 

In the chat message input box there is a icon with 2 little plugs. You
can use this to choose some build in prompts.  The only one of use
right now is the REPL driven development prompt `clojure-repl-driven.txt`.

### Using with Claude Desktop

Claude Desktop integrates with the Clojure MCP server, allowing Claude to:

1. **Evaluate Clojure code**: Claude can run code and see results immediately
2. **Access the filesystem**: Read and write Clojure files
3. **Access Git**: Interact with your Git repository (if configured)
4. **Navigate project structure**: Understand and modify your Clojure project

To connect Claude to your project:

1. Open Claude Desktop
2. Start a new conversation
3. Claude will automatically connect to the configured MCP servers
4. Use the little plug tool in the chat interface to bring up available prompts
5. Choose the clojure-repl-driven.txt 
6. Start a conversation initiating
7. Guide the development process

## Project Summary Management

This project includes a workflow for maintaining an LLM-friendly project summary that helps assistants quickly understand the codebase structure.

### How It Works

1. **Creating the Summary**: To generate or update the PROJECT_SUMMARY.md file, use the MCP prompt `create-project-summary`. This prompt will:
   - Analyze the codebase structure
   - Document key files, dependencies, and available tools
   - Generate comprehensive documentation in a format optimized for LLM assistants

2. **Using the Summary**: When starting a new conversation with an assistant:
   - The "Project Summary" resource automatically loads PROJECT_SUMMARY.md
   - This gives the assistant immediate context about the project structure
   - The assistant can provide more accurate help without lengthy exploration

3. **Keeping It Updated**: At the end of a productive session where new features or components were added:
   - Invoke the `create-project-summary` prompt again
   - The system will update the PROJECT_SUMMARY.md with newly added functionality
   - This ensures the summary stays current with ongoing development

This workflow creates a virtuous cycle where each session builds on the accumulated knowledge of previous sessions, making the assistant increasingly effective as your project evolves.

## Learning Curve

> This tool has a learning curve. You may in practice have to remind
> the LLM to develop in the REPL.  You may also have to remind the LLM
> to use the `clojure_edit` family of tools which have linters build
> in to prevent unbalanced parens and the like.

## 🧰 Available Tools

### Code Evaluation and Exploration

| Tool Name | Description | Example Usage |
|-----------|-------------|---------------|
| `clojure_eval` | Evaluates Clojure code | Evaluating `(+ 1 2)` returns `=> 3` |
| `current_namespace` | Shows active namespace | Returns `"user"` |
| `symbol_completions` | Provides completions | Finding all functions starting with "map" |
| `clojure_eval_history` | Shows recent evaluations | Retrieving last 5 evaluated expressions |

### Symbol Information

| Tool Name | Description | Example Usage |
|-----------|-------------|---------------|
| `symbol_documentation` | Gets documentation | Docs for `map` function |
| `symbol_metadata` | Gets symbol metadata | Complete metadata for `reduce` |
| `source_code` | Views source code | Source code for `filter` |
| `symbol-search` | Searches symbols | Finding all symbols containing "seq" |

### Namespace Tools

| Tool Name | Description | Example Usage |
|-----------|-------------|---------------|
| `clojure_list_namespaces` | Lists all namespaces | Showing available namespaces |
| `clojure_list_vars_in_namespace` | Lists namespace vars | All public vars in `clojure.string` |

### File Editing Tools

| Tool Name | Description | Example Usage |
|-----------|-------------|---------------|
| `clojure_file_outline` | Shows file structure | Overview of a Clojure file |
| `clojure_edit_replace_form` | Replaces a form | Updating a function definition |
| `clojure_edit_insert_before_form` | Inserts before a form | Adding a helper function |
| `clojure_edit_insert_after_form` | Inserts after a form | Adding a new function |

### Project Tools

| Tool Name | Description | Example Usage |
|-----------|-------------|---------------|
| `clojure_inspect_project` | Analyzes project | Shows deps, structure, etc. |

## 📜 Development Practices

### Recommended Workflow

1. **Express the problem** - Clearly state what you want to solve
2. **Develop in the REPL** - Work through solutions incrementally
3. **Validate step-by-step** - Test each expression before moving on
4. **Save to files** - When the solution is working, save it properly
5. **Reload and verify** - Make sure the saved code works

### Best Practices

- **Small steps** - Prefer many small, valid steps over a few large steps
- **Functional approach** - Use pure functions that are easier to test and reason about
- **Human guidance** - Provide feedback to keep development on track
- **Test early** - Validate ideas directly in the REPL before committing to them

## 🔧 Project Maintenance

```bash
# Run tests
clojure -X:test

# Run specific test
clojure -X:test :dirs '["test"]' :include '"repl_tools_test"'

# Run linter
clojure -M:lint
```

## 📚 Philosophy

The core philosophy of this project is that:

1. **Tiny steps with rich feedback** lead to better quality code
2. **REPL-driven development** provides the highest quality feedback loop
3. **Keeping humans in the loop** ensures discernment and maintainable code
4. **Functional programming patterns** produce more maintainable AI-generated code
5. **Collaborative development** between humans and AI can lead to higher quality than either alone

## 📋 Prompts

The project includes several prompts that guide LLM behavior:

- `clojure-repl-driven` - REPL-driven development workflow

- `clojure_dev` - General Clojure development guidelines
- `clj-spec-driven-modifier` - Spec-driven development guidelines
- `clj-test-driven-modifier` - Test-driven development workflow
- `clj-set-project-dir` - Project context setting
- `clj-sync-namespace` - Namespace synchronization

## 🔍 Troubleshooting

### Common Issues

#### File Editing Issues

**Problem**: Claude is unable to edit or create files.
**Solution**:
- Verify the filesystem MCP server is properly configured
- Check file paths are correct and within the allowed directories
- Ensure proper permissions on directories
- Try using the specialized Clojure editing tools rather than direct file editing

## 📝 License

This is a private project for now. Not intended for public consumption.
