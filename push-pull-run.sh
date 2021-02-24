#!/bin/bash
# git push origin master
gnome-terminal -- /bin/bash -c 'ssh -X wuh-chwen@best-linux.cs.wisc.edu << EOF; ssh -X mininet-16.cs.wisc.edu << EOF; ls; exec bash'