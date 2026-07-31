# Karpenter vs Kubernetes Cluster Autoscaler

**Status:** Theory only. Not practically tested in this repository.
**Runbook:** Not available for this pattern.

This pattern is intentionally architecture-focused. It is designed for understanding and planning. It does not claim hands-on validation in this repository.

---

## 1. Overview

Kubernetes autoscaling has multiple layers:

- HPA scales pods horizontally.
- VPA adjusts pod resource requests.
- Cluster Autoscaler and Karpenter scale the node/infrastructure layer.

This pattern focuses only on **node autoscaling**.

---

## 2. Why Node Autoscaling Is Needed

Pods become `Pending` when the cluster does not have enough resources or matching node constraints.

Common reasons:

- insufficient CPU
- insufficient memory
- GPU requirements
- zone/architecture requirements
- affinity/anti-affinity constraints
- taints/tolerations mismatch

Node autoscalers solve this by adding nodes. They can also remove unused or underutilized nodes to reduce cost.

---

## 3. What Is Cluster Autoscaler?

Cluster Autoscaler is the traditional Kubernetes node autoscaler.

How it works at a high level:

- It watches for unschedulable pods.
- It scales **predefined node groups** up or down.
- In AWS EKS, these are usually Managed Node Groups or Auto Scaling Groups.

Scale-up behavior:

- pod cannot schedule
- autoscaler finds a suitable node group
- autoscaler increases group size
- new node joins cluster
- scheduler places pod

Scale-down behavior:

- identifies underutilized nodes
- checks whether pods can be moved safely
- respects PodDisruptionBudgets and eviction constraints
- drains and removes node

Cluster Autoscaler is mature and widely used, but less flexible because node types and groups must be planned in advance.

---

## 4. What Is Karpenter?

Karpenter is a newer node provisioning and lifecycle management solution.

How it works at a high level:

- watches pending/unschedulable pods
- reads pod requirements (CPU, memory, architecture, zones, affinity, taints/tolerations, topology constraints, capacity type)
- provisions suitable nodes directly from workload needs

In AWS EKS, Karpenter commonly uses resources such as:

- `NodePool`
- `EC2NodeClass`

Karpenter is often more flexible and can improve cost efficiency, but it introduces a separate configuration model and operational learning curve.

---

## 5. How Cluster Autoscaler Works

### Scale-up flow

1. A pod is created.
2. Kubernetes scheduler tries to place the pod.
3. No existing node has enough capacity or matching constraints.
4. Pod remains Pending.
5. Cluster Autoscaler detects the unschedulable pod.
6. It checks which predefined node group can fit the pod.
7. It asks the cloud provider to increase the node group size.
8. A new node joins the cluster.
9. Kubernetes scheduler places the pending pod.

### Scale-down flow

1. Cluster Autoscaler finds underutilized nodes.
2. It checks whether pods can be moved elsewhere.
3. It respects PodDisruptionBudgets and eviction rules.
4. It drains the node.
5. It removes the node from the node group.

---

## 6. How Karpenter Works

1. A pod is created.
2. Kubernetes scheduler cannot place the pod.
3. Pod remains Pending.
4. Karpenter observes the pending pod.
5. Karpenter reads pod scheduling requirements.
6. Karpenter evaluates allowed instance types, zones, capacity type, architecture, and constraints from NodePool and EC2NodeClass.
7. Karpenter provisions a suitable node directly.
8. The node joins the cluster.
9. Kubernetes scheduler schedules the pod.
10. Later, Karpenter can consolidate, expire, replace, or remove nodes when they are no longer optimal.

---

## 7. Architecture Comparison

| Dimension | Cluster Autoscaler | Karpenter |
|---|---|---|
| Provisioning model | Scales existing node groups | Direct workload-driven provisioning |
| Dependency on node groups | Strong dependency | Not tied to fixed node group model |
| Cloud provider support | Broad multi-cloud support | Strong on AWS EKS; provider maturity varies |
| Flexibility | Lower (predefined groups) | Higher (dynamic instance selection) |
| Provisioning speed | Good, but group-based | Often faster for dynamic workloads |
| Cost optimization | Depends on node group design | Better bin-packing and consolidation potential |
| Spot instance handling | Possible, but group planning needed | More dynamic Spot/On-Demand selection |
| Consolidation | Limited compared to Karpenter model | Built-in consolidation concepts |
| Operational maturity | Very mature and widely adopted | Growing maturity with newer model |
| Configuration complexity | Moderate | Higher (NodePool, NodeClass, disruption settings) |
| Multi-cloud suitability | Strong | Depends on provider integrations |
| AWS/EKS suitability | Strong | Very strong |
| Best fit use cases | Stable and predictable workloads | Bursty and mixed workloads |
| Limitations | Less dynamic, potential over-provisioning | More learning and governance required |

---

## 8. Advantages and Disadvantages

### A. Cluster Autoscaler

**Advantages**

- Mature and widely adopted
- Works across multiple cloud providers
- Easier for teams already using node groups
- Good fit for stable and predictable workloads
- Integrates well with managed node groups

**Disadvantages**

- Depends on predefined node groups
- Less flexible instance selection
- Can lead to over-provisioning if node groups are poorly designed
- Usually slower reaction than direct provisioning models
- Cost optimization is more limited than Karpenter-style consolidation
- Requires careful planning for workload profiles

### B. Karpenter

**Advantages**

- More flexible node provisioning
- Dynamic instance type selection
- Better for bursty and mixed workloads
- Strong support for modern EKS scaling patterns
- Better bin-packing opportunities
- Consolidation and lifecycle optimization support

**Disadvantages**

- More complex configuration model
- Requires understanding of NodePool, NodeClass, disruption, consolidation, and scheduling constraints
- AWS/EKS is the strongest use case
- Multi-cloud maturity may vary by provider
- Misconfiguration can cause unexpected provisioning or disruptions
- Teams need stronger observability and governance before production rollout

---

## 9. When To Use Cluster Autoscaler

Use Cluster Autoscaler when:

- platform already uses managed node groups
- workloads are stable and predictable
- team wants a mature and conservative model
- multi-cloud compatibility is important
- organization already has approved node group standards
- operational simplicity is more important than maximum optimization
- cluster has a small number of known workload profiles

---

## 10. When To Use Karpenter

Use Karpenter when:

- platform runs mainly on AWS EKS
- workloads are dynamic, bursty, or mixed
- faster provisioning matters
- cost optimization and bin-packing matter
- flexible instance selection is needed
- Spot and On-Demand decisions should be dynamic
- platform team is comfortable with CRDs and advanced scheduling concepts
- environment has strong observability, governance, and disruption policies

---

## 11. Best of Both Worlds

Both can exist in one cluster, but responsibilities must be separated clearly.

General guidance:

- avoid letting both tools manage the same capacity pool
- use labels, taints, tolerations, and node selectors to enforce boundaries
- keep workload placement rules explicit

Example separation model:

- baseline critical workloads on managed node groups with Cluster Autoscaler
- dynamic/bursty workloads on Karpenter-managed capacity

Operational risk:

If boundaries are unclear, both autoscalers can make conflicting decisions.

---

## 12. Practical Scenarios

### Scenario 1: Stable business application with predictable traffic

Recommendation: **Cluster Autoscaler**

Reason: fixed node group capacity behavior is simpler and predictable.

### Scenario 2: High traffic burst during peak events

Recommendation: **Karpenter**

Reason: dynamic instance selection and faster reaction to large pending pod spikes.

### Scenario 3: Mixed workloads with different CPU and memory requirements

Recommendation: **Karpenter**

Reason: better workload-fit node provisioning and bin-packing.

### Scenario 4: Strict enterprise-approved node groups

Recommendation: **Cluster Autoscaler**

Reason: aligns with controlled node group standards and governance models.

### Scenario 5: Spot-heavy cost optimization on AWS EKS

Recommendation: **Karpenter**

Reason: better dynamic Spot/On-Demand capacity decisions.

### Scenario 6: Multi-cloud Kubernetes architecture

Recommendation: **Cluster Autoscaler**

Reason: broader and mature multi-cloud support profile.

### Scenario 7: Baseline plus burst model

Recommendation: **Both, with strict separation**

Reason: stable baseline on node groups, dynamic burst on Karpenter-managed pools.

---

## 13. Example Kubernetes Objects (Conceptual Snippets)

These are examples for design discussion and future manual validation.

### A. Example Deployment that can trigger scale-out

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: scaleout-demo
  namespace: default
spec:
  replicas: 12
  selector:
    matchLabels:
      app: scaleout-demo
  template:
    metadata:
      labels:
        app: scaleout-demo
    spec:
      containers:
        - name: app
          image: nginx:1.25-alpine
          resources:
            requests:
              cpu: "500m"
              memory: "512Mi"
            limits:
              cpu: "1"
              memory: "1Gi"
```

### B. Example nodeSelector usage

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: node-selector-demo
spec:
  replicas: 2
  selector:
    matchLabels:
      app: node-selector-demo
  template:
    metadata:
      labels:
        app: node-selector-demo
    spec:
      nodeSelector:
        node.kubernetes.io/instance-type: m6i.large
      containers:
        - name: app
          image: nginx:1.25-alpine
```

### C. Example tolerations and taints

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dedicated-workload
spec:
  replicas: 2
  selector:
    matchLabels:
      app: dedicated-workload
  template:
    metadata:
      labels:
        app: dedicated-workload
    spec:
      tolerations:
        - key: "workload"
          operator: "Equal"
          value: "batch"
          effect: "NoSchedule"
      containers:
        - name: app
          image: nginx:1.25-alpine
```

### D. Example PodDisruptionBudget

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: app-pdb
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: scaleout-demo
```

### E. Example Karpenter NodePool

```yaml
apiVersion: karpenter.sh/v1
kind: NodePool
metadata:
  name: workloads-general
spec:
  template:
    metadata:
      labels:
        capacity-class: dynamic
    spec:
      nodeClassRef:
        group: karpenter.k8s.aws
        kind: EC2NodeClass
        name: default-ec2
      requirements:
        - key: kubernetes.io/arch
          operator: In
          values: ["amd64"]
        - key: karpenter.sh/capacity-type
          operator: In
          values: ["spot", "on-demand"]
  disruption:
    consolidationPolicy: WhenEmptyOrUnderutilized
```

### F. Example Karpenter EC2NodeClass

```yaml
apiVersion: karpenter.k8s.aws/v1
kind: EC2NodeClass
metadata:
  name: default-ec2
spec:
  amiFamily: AL2023
  role: KarpenterNodeRole-CLUSTER_NAME
  subnetSelectorTerms:
    - tags:
        karpenter.sh/discovery: CLUSTER_NAME
  securityGroupSelectorTerms:
    - tags:
        karpenter.sh/discovery: CLUSTER_NAME
  tags:
    managed-by: karpenter
```

### G. Example Cluster Autoscaler node group concept

A typical node group definition usually contains:

- min size
- max size
- desired capacity
- instance type(s)
- labels
- taints

Conceptual example values:

- min size: 2
- max size: 20
- desired capacity: 4
- instance type: m6i.large
- labels: `workload=general`, `capacity-class=baseline`
- taints: optional, for dedicated pools

---

## 14. Useful Commands For Manual Testing

Use these as manual commands only.

### General Kubernetes commands

```bash
kubectl get pods -A
kubectl get pods -A --field-selector=status.phase=Pending
kubectl describe pod <pod-name> -n <namespace>
kubectl get nodes
kubectl describe node <node-name>
kubectl top nodes
kubectl top pods -A
kubectl get events -A --sort-by=.lastTimestamp
kubectl get pdb -A
kubectl get deployment -A
kubectl scale deployment <deployment-name> --replicas=<count> -n <namespace>
```

### Karpenter-oriented commands

```bash
kubectl get nodepool
kubectl describe nodepool <name>
kubectl get nodeclaims
kubectl describe nodeclaim <name>
kubectl get ec2nodeclass
kubectl describe ec2nodeclass <name>
```

### Cluster Autoscaler-oriented commands

```bash
kubectl get deployment -n kube-system
kubectl logs -n kube-system deployment/cluster-autoscaler
kubectl describe configmap cluster-autoscaler-status -n kube-system
```

### AWS EKS context commands for future manual validation

```bash
aws eks describe-cluster --name <cluster-name> --region <region>
aws eks list-nodegroups --cluster-name <cluster-name> --region <region>
aws autoscaling describe-auto-scaling-groups --region <region>
```

---

## 15. Decision Matrix

| Requirement | Better Choice | Reason |
|---|---|---|
| AWS EKS dynamic workloads | Karpenter | Dynamic provisioning and instance flexibility |
| Multi-cloud support | Cluster Autoscaler | More mature multi-cloud support model |
| Predictable enterprise workloads | Cluster Autoscaler | Node group governance aligns well |
| Fast scale-out | Karpenter | Direct workload-driven provisioning |
| Cost optimization | Karpenter | Better consolidation and bin-packing potential |
| Simple operations | Cluster Autoscaler | Familiar node group operational model |
| Spot optimization | Karpenter | Better dynamic Spot/On-Demand selection |
| Strict node group governance | Cluster Autoscaler | Designed around approved node groups |
| Mixed workload sizes | Karpenter | Better fit-to-workload provisioning |
| Mature production default | Cluster Autoscaler | Long operational history and broad adoption |

---

## 16. Final Recommendation

Use **Cluster Autoscaler** when:

- environment is stable
- node groups are already well-defined
- multi-cloud portability matters
- team prefers mature and conservative operations

Use **Karpenter** when:

- running mainly on AWS EKS
- workloads are dynamic or bursty
- cost optimization and faster provisioning are key goals
- platform team is ready to operate NodePools, disruption settings, and advanced scheduling constraints

For advanced platform architectures, both can exist in the same cluster only with strict boundary separation.

For this reference pattern, this content is **theory-only** and intended for architectural understanding and future validation.

---

## 17. Pattern Notes

- Theory-only reference architecture.
- No practical validation claim in this repository.
- No runbook for this pattern.
- No scripts are provided by design.

---

## Navigation

- [Back to Kubernetes Reference Architectures](../README.md)
- [Back to main repository](../../README.md)
