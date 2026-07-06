# Argo CD Learning Lab

This folder contains three complete Argo CD demo tracks:
- CLI demo: create Application resources directly from command line
- App of Apps demo: one parent Application creates child Applications
- ApplicationSet demo: one ApplicationSet generates multiple Applications

## Folder Layout

```text
argocd-practise/
├── cli-demo/
│   ├── k8s/
│   └── argocd/
├── app-of-apps-demo/
│   ├── k8s/
│   └── argocd/
├── applicationset-demo/
│   ├── k8s/
│   └── argocd/
└── auth-test.txt
```

## Prerequisites

- A running Kubernetes cluster
- Argo CD installed in namespace `argocd`
- kubectl access to the cluster
- Repo changes pushed to `main` (the manifests use `targetRevision: main`)

Quick check commands:

```bash
kubectl get ns argocd
kubectl get pods -n argocd
```

## Demo 1: CLI Application Demo

Goal: learn the direct way to register apps in Argo CD.

Argo manifests:
- argocd-practise/cli-demo/argocd

Workload manifests:
- argocd-practise/cli-demo/k8s

### Positive scenario

1. Apply all Argo Application manifests.

```bash
kubectl apply -f argocd-practise/cli-demo/argocd
```

2. Verify Applications were created.

```bash
kubectl get applications -n argocd
```

3. Verify workloads in app namespaces.

```bash
kubectl get all -n httpd-demo
kubectl get all -n nginx-demo
kubectl get all -n whoami-demo
kubectl get all -n logstorm-demo
```

Expected result:
- Applications become Synced/Healthy
- Pods and Services come up in each namespace

### Negative scenario

1. Edit one app source path to an invalid path in one file under `cli-demo/argocd`.
2. Commit and push.
3. Observe Application status in Argo CD.

Expected result:
- One app moves to OutOfSync/Degraded
- Argo CD reports render/path error

Recovery:
1. Restore the correct path.
2. Commit and push.
3. Sync again and verify app returns to Healthy.

## Demo 2: App of Apps Demo

Goal: learn parent-child orchestration using Applications.

Parent manifest:
- argocd-practise/app-of-apps-demo/argocd/app-of-apps-parent.yaml

Child manifests:
- argocd-practise/app-of-apps-demo/argocd/children

Workload manifests:
- argocd-practise/app-of-apps-demo/k8s

### Positive scenario

1. Apply only the parent Application.

```bash
kubectl apply -f argocd-practise/app-of-apps-demo/argocd/app-of-apps-parent.yaml
```

2. Verify parent and children were created.

```bash
kubectl get applications -n argocd
```

3. Verify workloads in child namespaces.

```bash
kubectl get all -n aoa-alpha-nginx
kubectl get all -n aoa-beta-httpd
kubectl get all -n aoa-gamma-whoami
```

Expected result:
- Parent app creates and manages child apps
- Child apps sync workloads automatically

### Negative scenario

1. Remove one child file from `app-of-apps-demo/argocd/children/kustomization.yaml`.
2. Commit and push.
3. Observe missing child app in Argo CD.

Expected result:
- Removed child app is pruned (if prune is enabled) or no longer managed by parent
- Team learns parent controls child app inventory

Recovery:
1. Add the child back to `kustomization.yaml`.
2. Commit and push.
3. Parent recreates/manages child again.

## Demo 3: ApplicationSet Demo

Goal: learn scalable app generation from one manifest.

ApplicationSet manifest:
- argocd-practise/applicationset-demo/argocd/applicationset-demo.yaml

Workload manifests:
- argocd-practise/applicationset-demo/k8s

### Positive scenario

1. Apply the ApplicationSet.

```bash
kubectl apply -f argocd-practise/applicationset-demo/argocd/applicationset-demo.yaml
```

2. Verify generated Applications.

```bash
kubectl get applications -n argocd | grep '^aset-'
```

3. Verify workloads in generated namespaces.

```bash
kubectl get all -n aset-nginx
kubectl get all -n aset-httpd
kubectl get all -n aset-whoami
```

Expected result:
- One ApplicationSet creates multiple Applications
- All generated apps sync independently

### Negative scenario

1. Break one element path in the list generator.
2. Commit and push.
3. Observe exactly one generated app failing while others remain healthy.

Expected result:
- Blast radius is limited to one generated app
- Demonstrates safer scaling model

Recovery:
1. Fix the broken path.
2. Commit and push.
3. ApplicationSet reconciles and app returns to Healthy.

## What To Practice Across All Three

1. Drift correction
- Manually change a Deployment replica count in cluster
- Observe Argo self-heal restoring desired state

2. GitOps rollback
- Change image tag to a bad tag
- Observe failure
- Revert commit and watch recovery

3. Namespace and access boundaries
- Use dedicated namespaces per demo app
- Show clear ownership and blast-radius isolation

4. Change propagation speed
- Compare how quickly each pattern updates apps after Git push

## Suggested Learning Order

1. Start with CLI demo to understand basic Application CRD behavior
2. Move to App of Apps to understand composition and app grouping
3. Finish with ApplicationSet to understand scalable app generation

## Handy Commands

```bash
kubectl get applications -n argocd
kubectl describe applications -n argocd <app-name>
kubectl get events -n argocd --sort-by=.metadata.creationTimestamp
kubectl logs -n logstorm-demo -l app=logstorm-demo -f --tail=50
```

## Troubleshooting Tips

- If apps stay OutOfSync, verify `repoURL`, `targetRevision`, and `path`
- If no resources appear, verify Argo CD has repo access and correct permissions
- If ApplicationSet does not generate apps, check ApplicationSet controller pod in `argocd` namespace
