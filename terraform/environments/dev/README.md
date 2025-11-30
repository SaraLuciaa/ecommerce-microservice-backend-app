# Terraform + DigitalOcean Kubernetes Setup for Dev Environment

## Overview
This is the **DEVELOPMENT** environment configuration that creates a DigitalOcean Kubernetes (DOKS) cluster and deploys all ecommerce microservices using Terraform Cloud.

⚠️ **Start here before deploying to stage or prod!**

## Prerequisites

1. **DigitalOcean Account & API Token**
   - Create a DigitalOcean account at https://cloud.digitalocean.com
   - Generate a Personal Access Token with read/write permissions
   - Go to: API → Tokens/Keys → Generate New Token

2. **Terraform Cloud Account**
   - Sign up at https://app.terraform.io
   - Create organization: `ingesoffttt`
   - Create workspace: `ecommerce-dev`

## Setup Instructions

### 1. Configure Terraform Cloud Variables

In your Terraform Cloud workspace (`ecommerce-dev`), add the following variable:

**Environment Variables:**
- `do_token` (sensitive) = `dop_v1_your_digitalocean_token_here`

**Terraform Variables (optional overrides):**
- `cluster_name` = `"ecommerce-dev-cluster"`
- `region` = `"nyc1"`
- `node_size` = `"s-2vcpu-2gb"` (smaller/cheaper for dev)
- `node_count` = `2`

### 2. Verify Configuration Files

Ensure these files exist in `terraform/environments/dev/`:
- ✅ `backend.tf` - Terraform Cloud backend configuration
- ✅ `provider.tf` - DigitalOcean & Kubernetes providers
- ✅ `variables.tf` - Variable definitions
- ✅ `k8s-cluster.tf` - DOKS cluster resource
- ✅ `main.tf` - Microservices deployments
- ✅ `outputs.tf` - Cluster information outputs

### 3. Deploy Infrastructure

#### Option A: Via Terraform Cloud UI
1. Push code to `develop` branch
2. Go to Terraform Cloud workspace: `ecommerce-dev`
3. Click "Actions" → "Start new run"
4. Review plan (should create ~20 resources)
5. Click "Confirm & Apply"
6. Wait ~5-10 minutes for cluster creation

#### Option B: Via GitHub Actions (if configured)
1. Push to `develop` branch
2. GitHub Actions will trigger automatically
3. Monitor workflow in Actions tab

### 4. Verify Deployment

After successful apply, check outputs:
```bash
# View outputs in Terraform Cloud UI or run:
terraform output

# Expected outputs:
# - cluster_id: doks-xxx-xxx-xxx
# - cluster_endpoint: https://xxx.k8s.ondigitalocean.com
# - cluster_name: ecommerce-dev-cluster
# - cluster_status: running
```

### 5. Access Your Cluster

#### Download kubeconfig:
```bash
# Install doctl if not already installed
# Windows: choco install doctl
# Mac: brew install doctl
# Linux: snap install doctl

doctl auth init
doctl kubernetes cluster kubeconfig save <cluster_id>
```

#### Verify pods are running:
```bash
kubectl get pods -n dev
# Wait for all pods to be Running (may take 3-5 minutes)

kubectl get svc -n dev
```

#### Get API Gateway external IP:
```bash
kubectl get svc api-gateway-container -n dev -w
# Wait for EXTERNAL-IP (not <pending>)
# Then access: http://<EXTERNAL-IP>:8080
```

#### Test the API:
```bash
# Once you have the external IP:
curl http://<EXTERNAL-IP>:8080/actuator/health

# Check Eureka dashboard:
# http://<service-discovery-external-ip>:8761
```

## Architecture

```
DigitalOcean Cloud (Dev)
└── Kubernetes Cluster (DOKS)
    └── Namespace: dev
        ├── zipkin (9411)
        ├── service-discovery (8761)
        ├── cloud-config (9296)
        ├── api-gateway (8080) [LoadBalancer]
        ├── order-service (8300)
        ├── payment-service (8400)
        ├── product-service (8500)
        ├── shipping-service (8600)
        ├── user-service (8700)
        ├── favourite-service (8800)
        └── proxy-client (8900)
```

## Cost Estimate (Dev Environment)

- **Cluster:** ~$12/month (included)
- **2x s-2vcpu-2gb nodes:** ~$24/month ($12 each)
- **LoadBalancer:** ~$12/month
- **Total:** ~$36/month

💡 **Tip:** Destroy the cluster when not in use to save costs!

## Troubleshooting

### Issue: "Error creating Kubernetes cluster"
- Verify `do_token` is set correctly in Terraform Cloud (Environment Variable, marked as sensitive)
- Check DigitalOcean account has sufficient quota
- Ensure selected region supports Kubernetes
- Try a different region (e.g., `sfo3`, `sgp1`)

### Issue: "Provider configuration not found"
- Ensure `provider.tf` references the cluster resource correctly
- The Kubernetes provider depends on the cluster being created first

### Issue: Pods stuck in Pending
- Check node resources: `kubectl describe nodes`
- Check pod events: `kubectl describe pod <pod-name> -n dev`
- Nodes might be too small - increase `node_size` to `s-2vcpu-4gb`

### Issue: Can't access API Gateway
- Wait 1-2 minutes for LoadBalancer provisioning
- Check service: `kubectl get svc api-gateway-container -n dev`
- Verify DigitalOcean firewall rules allow inbound traffic
- Check pod logs: `kubectl logs -f deployment/api-gateway-container -n dev`

### Issue: Pods restarting frequently
- Readiness/liveness probes might be too aggressive (250s initial delay)
- Check logs: `kubectl logs <pod-name> -n dev`
- Increase resource limits if OOMKilled

## Development Workflow

1. **Make code changes** to your microservices
2. **Build and push** new Docker images with `:dev` tag
3. **Update Terraform** `main.tf` if needed (or use CI/CD)
4. **Apply changes**: Go to Terraform Cloud → Queue Plan
5. **Verify deployment**: Check pods and test endpoints

## Cleanup

### Temporary Shutdown (keep cluster, remove services):
```bash
kubectl delete namespace dev
# or via Terraform:
# Comment out all modules in main.tf except cluster
# terraform apply
```

### Full Destroy (delete everything):
1. Go to Terraform Cloud workspace: `ecommerce-dev`
2. Settings → Destruction and Deletion
3. Click "Queue destroy plan"
4. Review and confirm deletion
5. **Warning:** This deletes the cluster and all data!

Cost: $0/month when destroyed 💰

## Security Notes

- ✅ API token stored securely in Terraform Cloud (encrypted)
- ✅ No kubeconfig files in repository
- ✅ Provider configured dynamically from cluster
- ⚠️ LoadBalancer exposes services publicly (use Ingress + TLS in prod)
- ⚠️ No database persistence configured (use PVs for prod)
- ⚠️ Secrets hardcoded as env vars (use Kubernetes Secrets)

## Next Steps for Stage/Prod

After validating in dev:
1. ✅ Implement Kubernetes Secrets for sensitive data
2. ✅ Add Persistent Volumes for databases
3. ✅ Configure Ingress Controller (nginx) instead of LoadBalancer
4. ✅ Add ConfigMaps for application config
5. ✅ Enable Horizontal Pod Autoscaling
6. ✅ Implement Network Policies
7. ✅ Add monitoring (Prometheus/Grafana)
8. ✅ Use larger nodes (s-4vcpu-8gb)
9. ✅ Increase replica counts

## Useful Commands

```bash
# Watch all pods
kubectl get pods -n dev -w

# Get all resources
kubectl get all -n dev

# Describe a deployment
kubectl describe deployment api-gateway-container -n dev

# View logs
kubectl logs -f deployment/api-gateway-container -n dev

# Execute into a pod
kubectl exec -it <pod-name> -n dev -- /bin/sh

# Port-forward for local testing
kubectl port-forward svc/api-gateway-container 8080:8080 -n dev

# Scale a deployment
kubectl scale deployment order-service-container --replicas=3 -n dev

# Restart a deployment
kubectl rollout restart deployment/api-gateway-container -n dev

# Check cluster info
kubectl cluster-info
doctl kubernetes cluster list
```

## Additional Resources

- [DigitalOcean Kubernetes Docs](https://docs.digitalocean.com/products/kubernetes/)
- [Terraform DigitalOcean Provider](https://registry.terraform.io/providers/digitalocean/digitalocean/latest/docs)
- [Terraform Cloud Docs](https://developer.hashicorp.com/terraform/cloud-docs)
- [DigitalOcean Pricing](https://www.digitalocean.com/pricing/kubernetes)
