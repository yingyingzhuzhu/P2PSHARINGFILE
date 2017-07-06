This is a P2P file sharing project similar to BitTorrent.

1. Project Overview:
Developed in Java.
Implemented the choking-unchoking mechanism which is one of the most important features of BitTorrent.

2. Protocol Description:
All operations are assumed to be implemented using a reliable transport protocol (i.e. TCP). The interaction between two peers is symmetrical: Messages sent in both directions look the same.
The protocol consists of a handshake followed by a never-ending stream of length- prefixed messages.
Whenever a connection is established between two peers, each of the peers of the connection sends to the other one the handshake message before sending other messages.

