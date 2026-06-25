#!/bin/bash
##from chatgpt - use this to configure swap space on a RAMADDA AWS instance
##that has /mnt/ramadda
##Run:
##sudo bash setup_swap.sh

set -e

SWAPFILE="/mnt/ramadda/swapfile"
SWAPSIZE="8G"

echo "Creating swap file: $SWAPFILE"

if [ -f "$SWAPFILE" ]; then
    echo "Swap file already exists: $SWAPFILE"
    exit 1
fi

# Create swap file
if command -v fallocate >/dev/null 2>&1; then
    fallocate -l "$SWAPSIZE" "$SWAPFILE"
else
    echo "fallocate not found, using dd"
    dd if=/dev/zero of="$SWAPFILE" bs=1M count=8192 status=progress
fi

# Secure permissions
chmod 600 "$SWAPFILE"

# Format as swap
mkswap "$SWAPFILE"

# Enable immediately
swapon "$SWAPFILE"

# Add to fstab if not already present
if ! grep -q "$SWAPFILE" /etc/fstab; then
    echo "$SWAPFILE swap swap defaults 0 0" >> /etc/fstab
fi

# Set swappiness
mkdir -p /etc/sysctl.d
cat >/etc/sysctl.d/99-ramadda-swap.conf <<EOF
vm.swappiness=20
EOF

sysctl -p /etc/sysctl.d/99-ramadda-swap.conf

echo
echo "Swap configuration complete."
echo
free -h
echo
swapon --show
