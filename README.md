# Raymond Tree-Based Algorithm for Distributed Mutual Exclusion

This repository contains a implementation of the algorithm defined in the following scientific publication:

> -- <cite>Raymond, K. (1989). A tree-based algorithm for distributed mutual exclusion. ACM Transactions on Computer Systems (TOCS), 7(1), 61-77.</cite>

In order to understand the project read the paper and also the [report](https://github.com/armaniv/RaymondAlgorithm/blob/master/Report.pdf) which is meant to explain the main architectural choices of the implementation of Raymond tree based algorithm for distributed mutual exclusion [1] using AKKA framework and its testing. Details about the main procedures and requirements of the algorithm are omitted because well described in the original paper and can also be investigated in the source code. The implementation is based on a network of N nodes which interact each other using messages rather than shared memory

### Requirements: [AKKA](https://doc.akka.io/docs/akka/2.0.5/intro/getting-started.html)