#!/bin/bash 

set -exo pipefail

partition=1
snapshotsPath=data/raft-partition/partitions/$partition/snapshots

snapshotDir=$(ls -la $snapshotsPath | grep -o -E "([0-9]+-[0-9]+-[0-9]+)")

fileName=$(ls -la $snapshotsPath/$snapshotDir/ | sort -R | grep -o -m1 -E "[0-9]+\.sst")

rm $snapshotsPath/$snapshotDir/$fileName
