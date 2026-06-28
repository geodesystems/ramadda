#!/usr/bin/env bash
#
# ramadda_aws.sh
#
# Single helper script for setting up RAMADDA AWS aliases and for scp put/get.
#
# Alias setup, usually sourced from .bashrc:
#   source ${RAMADDA_BIN}/ramadda_aws.sh ramadda <IP> -pem ${RAMADDA_PEM}
#   source ${RAMADDA_BIN}/ramadda_aws.sh ramadda <IP> -pem ${RAMADDA_PEM} -user ec2-user -dir /mnt/ramadda/ramaddainstall
#
# Direct put:
#   ramadda_aws.sh -put <DEST_IP> <SOURCE_FILE_OR_DIR> -pem <pem file> -user <user> -dest <remote dest dir/file>
#   ramadda_aws.sh -put <DEST_IP> <SOURCE_FILE_OR_DIR> <remote dest dir/file>
#
# Direct get:
#   ramadda_aws.sh -get <SOURCE_IP> <REMOTE_FILE_OR_DIR> -pem <pem file> -user <user>
#
# Generated aliases for ID, e.g. ramadda:
#   ip_ramadda
#   goramadda
#   putramadda <local file/dir> [-dest <remote dest>]
#   getramadda <remote file/dir>
#   updateramadda
#   devupdateramadda
#   startramadda

usage() {
    cat <<'USAGE'
Usage:
  Source to define aliases:
    source ramadda_aws.sh <id> <ip> [-pem <pem file>] [-user <user>] [-dir <ramadda install dir>]

  Put a local file/dir to the server:
    ramadda_aws.sh -put <ip> <source file/dir> [-pem <pem file>] [-user <user>] [-dest <remote dest>]
    ramadda_aws.sh -put <ip> <source file/dir> <remote dest>

  Get a remote file/dir from the server:
    ramadda_aws.sh -get <ip> <remote file/dir> [-pem <pem file>] [-user <user>]

Defaults:
  user: ec2-user
  ramadda install dir: /mnt/ramadda/ramaddainstall

Results in generated aliases for ID, e.g. ramadda:
ip_ramadda
goramadda
putramadda <local file/dir> [-dest <remote dest>]
getramadda <remote file/dir>
updateramadda
devupdateramadda
startramadda

USAGE
}

script_dir() {
    local src
    src="${BASH_SOURCE[0]:-$0}"
    cd "$(dirname "$src")" >/dev/null 2>&1 && pwd
}

need_value() {
    local flag="$1"
    local value="${2-}"
    if [ -z "$value" ]; then
        echo "Missing value for $flag" >&2
        usage >&2
        return 1
    fi
}

run_put() {
    local ip="${1-}"
    local user="ec2-user"
    local pem=""
    local source=""
    local dest=""

    if [ -z "$ip" ]; then
        usage >&2
        return 1
    fi
    shift

    while [ $# -gt 0 ]; do
        case "$1" in
            -pem)
                need_value "$1" "${2-}" || return 1
                pem="$2"
                shift 2
                ;;
            -user)
                need_value "$1" "${2-}" || return 1
                user="$2"
                shift 2
                ;;
            -dest)
                need_value "$1" "${2-}" || return 1
                dest="$2"
                shift 2
                ;;
            -h|--help)
                usage
                return 0
                ;;
            *)
                if [ -n "$source" ]; then
                    # Backward-compatible with the old put.sh behavior where
                    # a second positional argument became the destination.
                    dest="$1"
                else
                    source="$1"
                fi
                shift
                ;;
        esac
    done

    if [ -z "$source" ]; then
        echo "Missing source file/dir for -put" >&2
        usage >&2
        return 1
    fi

    echo "scping $source to $user@$ip $dest"

    if [ -n "$pem" ]; then
        if [ -z "$dest" ]; then
            scp -r -i "$pem" "$source" "${user}@${ip}:"
        else
            scp -r -i "$pem" "$source" "${user}@${ip}:$dest"
        fi
    else
        if [ -z "$dest" ]; then
            scp -r "$source" "${user}@${ip}:"
        else
            scp -r "$source" "${user}@${ip}:$dest"
        fi
    fi
}

run_get() {
    local ip="${1-}"
    local user="ec2-user"
    local pem=""
    local source=""

    if [ -z "$ip" ]; then
        usage >&2
        return 1
    fi
    shift

    while [ $# -gt 0 ]; do
        case "$1" in
            -pem)
                need_value "$1" "${2-}" || return 1
                pem="$2"
                shift 2
                ;;
            -user)
                need_value "$1" "${2-}" || return 1
                user="$2"
                shift 2
                ;;
            -h|--help)
                usage
                return 0
                ;;
            *)
                source="$1"
                shift
                ;;
        esac
    done

    if [ -z "$source" ]; then
        echo "Missing remote source file/dir for -get" >&2
        usage >&2
        return 1
    fi

    echo "scping $source from $user@$ip"

    if [ -n "$pem" ]; then
        scp -r -i "$pem" "${user}@${ip}:$source" .
    else
        scp -r "${user}@${ip}:$source" .
    fi
}

define_aliases() {
    local id="${1-}"
    local ip="${2-}"
    local user="ec2-user"
    local pem=""
    local ramadda_install="/mnt/ramadda/ramaddainstall"
    local mydir

    if [ -z "$id" ] || [ -z "$ip" ]; then
        usage >&2
        return 1
    fi
    shift 2

    while [ $# -gt 0 ]; do
        case "$1" in
            -pem)
                need_value "$1" "${2-}" || return 1
                pem="$2"
                shift 2
                ;;
            -user)
                need_value "$1" "${2-}" || return 1
                user="$2"
                shift 2
                ;;
            -dir)
                need_value "$1" "${2-}" || return 1
                ramadda_install="$2"
                shift 2
                ;;
            -h|--help)
                usage
                return 0
                ;;
            *)
                echo "Unknown argument: $1" >&2
                usage >&2
                return 1
                ;;
        esac
    done

    mydir="$(script_dir)"

    alias "ip_${id}=echo $ip"

    if [ -n "$pem" ]; then
        alias "go${id}=ssh -i '$pem' ${user}@${ip}"
        alias "put${id}=bash '$mydir/ramadda_aws.sh' -put '$ip' -pem '$pem' -user '$user'"
        alias "get${id}=bash '$mydir/ramadda_aws.sh' -get '$ip' -pem '$pem' -user '$user'"

        eval "update${id}() {
            echo \"updating $id ${user}@${ip} install dir: ${ramadda_install}\"
            ssh -i '$pem' '${user}@${ip}' \"sudo bash ramaddainstaller/update.sh -dir '${ramadda_install}'\"
        }"
        eval "devupdate${id}() {
            echo \"updating $id ${user}@${ip} install dir: ${ramadda_install}\"
            ssh -i '$pem' '${user}@${ip}' \"sudo bash ramaddainstaller/update.sh -dev -dir '${ramadda_install}'\"
        }"
        eval "start${id}() {
            echo \"starting RAMADDA\"
            ssh -i '$pem' '${user}@${ip}' \"sudo service ramadda start\"
        }"
    else
        alias "go${id}=ssh ${user}@${ip}"
        alias "put${id}=bash '$mydir/ramadda_aws.sh' -put '$ip' -user '$user'"
        alias "get${id}=bash '$mydir/ramadda_aws.sh' -get '$ip' -user '$user'"

        eval "update${id}() {
            echo \"updating $id ${user}@${ip} install dir: ${ramadda_install}\"
            ssh '${user}@${ip}' \"sudo bash ramaddainstaller/update.sh -dir '${ramadda_install}'\"
        }"
        eval "devupdate${id}() {
            echo \"updating $id ${user}@${ip} install dir: ${ramadda_install}\"
            ssh '${user}@${ip}' \"sudo bash ramaddainstaller/update.sh -dev -dir '${ramadda_install}'\"
        }"
        eval "start${id}() {
            echo \"starting RAMADDA\"
            ssh '${user}@${ip}' \"sudo service ramadda start\"
        }"
    fi
}

main() {
    case "${1-}" in
        -put)
            shift
            run_put "$@"
            ;;
        -get)
            shift
            run_get "$@"
            ;;
        -h|--help|"")
            usage
            ;;
        *)
            define_aliases "$@"
            ;;
    esac
}

main "$@"
