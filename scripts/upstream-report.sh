#!/usr/bin/env bash
# Lists what Mihon and Tsundoku have added since Tsuzuki last took from them, grouped by area,
# and predicts which commits will conflict or touch Tsuzuki's design. See docs/UPSTREAM.md.
#
#   scripts/upstream-report.sh            # report on both upstreams
#   scripts/upstream-report.sh mihon      # just one
#
# Columns: area, flags, sha, date, subject.
#   T = touches a file Tsuzuki changed (check the design after picking; likely conflict)
#   D = touches a file Tsundoku changed from Mihon (likely conflict; Mihon only)
#   . = neither: usually picks cleanly
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

ensure_remote() {
    git remote get-url "$1" >/dev/null 2>&1 || git remote add "$1" "$2"
}
ensure_remote tsundoku https://github.com/tsundoku-otaku/tsundoku.git
ensure_remote mihon https://github.com/mihonapp/mihon.git

targets=("${@:-tsundoku mihon}")
# shellcheck disable=SC2206
targets=(${targets[*]})

for remote in "${targets[@]}"; do
    git fetch "$remote" main --no-tags -q
done

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

# Files Tsuzuki changed on top of Tsundoku, and files Tsundoku changed on top of Mihon.
git diff --name-only tsundoku/main...HEAD | sort -u > "$tmp/tsuzuki"
mihon_base=$(git merge-base tsundoku/main mihon/main)
git diff --name-only "$mihon_base" tsundoku/main | sort -u > "$tmp/tsundoku"

area_of() {
    case "$1" in
        *data/download/* | *DownloadPreferences* | *download*) echo download ;;
        *ui/reader/* | *presentation/reader/* | *viewer/*) echo reader ;;
        *library/*) echo library ;;
        *backup/*) echo backup ;;
        *track/*) echo tracking ;;
        *extension/* | *browse/* | *source/*) echo browse ;;
        *network/* | *Cloudflare* | *webview/* | *WebView*) echo network ;;
        *.sq | *.sqm | data/src/*) echo database ;;
        i18n/*) echo strings ;;
        *.gradle.kts | *.toml | .github/* | gradle/*) echo build ;;
        *presentation/* | *ui/*) echo ui ;;
        *) echo other ;;
    esac
}

for remote in "${targets[@]}"; do
    count=$(git rev-list --count --no-merges --cherry-pick --right-only "HEAD...$remote/main")
    echo
    echo "## $remote/main: $count commits not in Tsuzuki"
    echo
    git log --no-merges --cherry-pick --right-only --reverse --format='%h %ad %s' --date=short "HEAD...$remote/main" |
        while read -r sha date subject; do
            # Skip noise: dependency bumps and translation syncs are taken wholesale or not at all.
            case "$subject" in
                Update\ dependency* | Update\ *\ to\ v* | Translations\ update* | Bump\ * | Release\ v*) continue ;;
            esac
            files=$(git show --name-only --format= "$sha")
            flags=""
            grep -qxFf "$tmp/tsuzuki" <<<"$files" && flags+="T"
            [ "$remote" = mihon ] && grep -qxFf "$tmp/tsundoku" <<<"$files" && flags+="D"
            area=$(area_of "$(head -1 <<<"$files")")
            printf '%-9s %-3s %s %s %s\n' "$area" "${flags:-.}" "$sha" "$date" "$subject"
        done | sort -s -k1,1
done
