# Devbox Runtime Isolation Guide

This project uses `devbox` to isolate runtime versions per repository so local global state does not affect builds or tests.

## Why Devbox Here
- Pin Node and Java per project in `devbox.json`.
- Keep command entrypoints stable through `devbox run <script>`.
- Avoid cross-project drift from global `nvm`, `JAVA_HOME`, or shell profile differences.
- Make local workflows closer to reproducible CI-style execution.

## How It Works
1. `devbox.json` declares packages and project scripts.
2. `devbox install` resolves packages through Nix and stores results in `devbox.lock`.
3. `devbox run ...` executes inside that pinned environment.
4. Other projects can use different `devbox.json` values without conflicts.

## Install Devbox
```bash
curl -fsSL https://get.jetify.com/devbox | bash
devbox version
```

If you prefer user-local installation without writing `/usr/local/bin`:
```bash
mkdir -p "$HOME/.local/bin"
curl -fsSL https://releases.jetify.com/devbox -o "$HOME/.local/bin/devbox"
chmod +x "$HOME/.local/bin/devbox"
"$HOME/.local/bin/devbox" version
```
Add user-local bin to shell PATH if needed:
```bash
echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

## Bootstrap This Repository
```bash
cd /Users/jixu/Desktop/codex-springboot-react
devbox install
devbox run doctor
```
On macOS, first-time Nix bootstrap usually requires interactive `sudo`.

## Daily Commands
```bash
devbox run frontend-install
devbox run backend-build
devbox run frontend-build
devbox run frontend-lint
devbox run frontend-test
devbox run frontend-e2e
```

## Full Verification
```bash
devbox run verify-all
```

## Optional Auto-Activation
If you use `direnv`, generate `.envrc` once:
```bash
devbox generate direnv
direnv allow
```

## Notes
- `.nvmrc` and `.java-version` remain as fallback hints, but `devbox.json` is the source of truth for runtime versions in this repository.
- Commit `devbox.lock` after `devbox install` to lock exact package resolution for the team.
- GitHub Actions workflows in this repository also run through `devbox`, so local and CI command entrypoints stay aligned.
