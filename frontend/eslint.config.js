import { defineConfig } from 'eslint/config'
import reactHooks from 'eslint-plugin-react-hooks'
import tseslint from 'typescript-eslint'

export default defineConfig(
  { ignores: ['dist'] },
  tseslint.configs.recommended,
  {
    plugins: { 'react-hooks': reactHooks },
    rules: reactHooks.configs.recommended.rules,
  },
  {
    languageOptions: {
      parserOptions: { warnOnUnsupportedTypeScriptVersion: false },
    },
  },
)
