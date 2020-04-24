#!/bin/bash

set -xoe pipefail


kubectl exec zell-chaos-zeebe-0 -- zbctl status --insecure
#k exec -it zell-chaos-zeebe-2 bash < script.sh

