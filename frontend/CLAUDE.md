# Frontend Development Rules

## Package Management

- **Use exact version specifiers** for all packages in `package.json`. Do not use range prefixes (`^`, `~`, `>=`, etc.). Every dependency and devDependency must be pinned to a specific version (e.g., `"axios": "1.13.5"`, not `"axios": "^1.13.5"`).
