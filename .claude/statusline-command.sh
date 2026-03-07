#!/bin/bash

# Claude Code が stdin に送信する JSON データを読み取る
# https://code.claude.com/docs/en/statusline#available-data
input=$(cat)

# jq を使用してフィールドを抽出する
MODEL=$(echo "$input" | jq -r '.model.display_name // "Claude"')
DIR=$(echo "$input" | jq -r '.workspace.current_dir')
USED_PCT=$(echo "$input" | jq -r '.context_window.used_percentage // 0' | cut -d. -f1)

INPUT_TOKENS=$(echo "$input" | jq -r '.context_window.total_input_tokens // "0"')
OUTPUT_TOKENS=$(echo "$input" | jq -r '.context_window.total_output_tokens // "0"')
DURATION_MS=$(echo "$input" | jq -r '.cost.total_api_duration_ms // "0"')
LATENCY=$(echo "scale=1; $DURATION_MS / 1000" | bc)

GREEN='\033[32m'
YELLOW='\033[33m'
RESET='\033[0m'

if git -C "${DIR:-$PWD}" rev-parse --git-dir > /dev/null 2>&1; then
    BRANCH=$(git -C "${DIR:-$PWD}" branch --show-current 2>/dev/null)
    STAGED=$(git -C "${DIR:-$PWD}" diff --cached --numstat 2>/dev/null | wc -l | tr -d ' ')
    MODIFIED=$(git -C "${DIR:-$PWD}" diff --numstat 2>/dev/null | wc -l | tr -d ' ')

    GIT_STATUS=""
    [ "$STAGED" -gt 0 ] && GIT_STATUS="${GREEN}+${STAGED}${RESET}"
    [ "$MODIFIED" -gt 0 ] && GIT_STATUS="${GIT_STATUS}${YELLOW}~${MODIFIED}${RESET}"

    echo -e "📁 ${DIR##*/} | 🌿 $BRANCH $GIT_STATUS"
else
    echo "📁 ${DIR##*/}"
fi

echo "${MODEL} | ${INPUT_TOKENS}/${OUTPUT_TOKENS} tokens | Context: ${USED_PCT}% used | ${LATENCY}s"
