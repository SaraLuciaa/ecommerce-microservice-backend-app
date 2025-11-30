# Terraform + DigitalOcean Kubernetes Setup for Stage Environment

## Overview
This configuration creates a DigitalOcean Kubernetes (DOKS) cluster and deploys all ecommerce microservices to it using Terraform Cloud.

## Prerequisites

1. **DigitalOcean Account & API Token**
   - Create a DigitalOcean account at https://cloud.digitalocean.com
   - Generate a Personal Access Token with read/write permissions
   - Go to: API → Tokens/Keys → Generate New Token

2. **Terraform Cloud Account**
   - Sign up at https://app.terraform.io
   - Create organization: `ingesoffttt`
   - Create workspace: `ecommerce-stage`

## Setup Instructions

### 1. Configure Terraform Cloud Variables

In your Terraform Cloud workspace (`ecommerce-stage`), add the following variable:

**Environment Variables:**
- `do_token` (sensitive) = `dop_v1_your_digitalocean_token_here`

**Terraform Variables (optional overrides):**
- `cluster_name` = `"ecommerce-stage-cluster"`
- `region` = `"nyc1"`
- `node_size` = `"s-2vcpu-4gb"`
- `node_count` = `2`

### 2. Verify Configuration Files

Ensure these files exist in `terraform/environments/stage/`:
- ✅ `backend.tf` - Terraform Cloud backend configuration
- ✅ `provider.tf` - DigitalOcean & Kubernetes providers
- ✅ `variables.tf` - Variable definitions
- ✅ `k8s-cluster.tf` - DOKS cluster resource
- ✅ `main.tf` - Microservices deployments
- ✅ `outputs.tf` - Cluster information outputs

### 3. Deploy Infrastructure

#### Option A: Via Terraform Cloud UI
1. Push code to `stage` branch
2. Go to Terraform Cloud workspace
3. Click "Actions" → "Start new run"
4. Review plan
5. Click "Confirm & Apply"

#### Option B: Via GitHub Actions (if configured)
1. Push to `stage` branch
2. GitHub Actions will trigger automatically
3. Monitor workflow in Actions tab

### 4. Verify Deployment

After successful apply, check outputs:
```bash
# View outputs in Terraform Cloud UI or run:
terraform output

# Expected outputs:
# - cluster_id
# - cluster_endpoint
# - cluster_name
# - cluster_status
```

### 5. Access Your Cluster

#### Download kubeconfig:
```bash
# Install doctl if not already installed
# See: https://docs.digitalocean.com/reference/doctl/how-to/install/

doctl auth init
doctl kubernetes cluster kubeconfig save <cluster_id>
```

#### Verify pods are running:
```bash
kubectl get pods -n stage
kubectl get svc -n stage
```

#### Get API Gateway external IP:
```bash
kubectl get svc api-gateway-container -n stage
# Look for EXTERNAL-IP column (may take 1-2 minutes)
```

## Architecture

```
DigitalOcean Cloud
└── Kubernetes Cluster (DOKS)
    └── Namespace: stage
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

## Cost Estimate

- **Cluster:** ~$12/month (included)
- **2x s-2vcpu-4gb nodes:** ~$36/month ($18 each)
- **LoadBalancer:** ~$12/month
- **Total:** ~$48/month

## Troubleshooting

### Issue: "Error creating Kubernetes cluster"
- Verify `do_token` is set correctly in Terraform Cloud
- Check DigitalOcean account has sufficient quota
- Ensure selected region supports Kubernetes

### Issue: "Provider configuration not found"
- Ensure `provider.tf` references the cluster resource correctly
- Run `terraform init` to download providers

### Issue: Pods stuck in Pending
- Check node resources: `kubectl describe nodes`
- Scale nodes if needed (update `node_count` variable)

### Issue: Can't access API Gateway
- Wait 1-2 minutes for LoadBalancer provisioning
- Check service: `kubectl get svc api-gateway-container -n stage`
- Verify DigitalOcean firewall rules

## Cleanup

To destroy all resources:
1. Go to Terraform Cloud workspace
2. Settings → Destruction and Deletion
3. Click "Queue destroy plan"
4. Confirm deletion

**Warning:** This will delete the cluster and all deployed services!

## Security Notes

- ✅ API token stored securely in Terraform Cloud (encrypted)
- ✅ No kubeconfig files in repository
- ✅ Provider configured dynamically from cluster
- ✅ All secrets should be managed via Kubernetes Secrets (not implemented yet)

## Next Steps

Consider implementing:
- [ ] Kubernetes Secrets for database credentials
- [ ] Persistent Volumes for MySQL/PostgreSQL
- [ ] Ingress Controller (nginx) instead of multiple LoadBalancers
- [ ] ConfigMaps for application configuration
- [ ] Horizontal Pod Autoscaling
- [ ] Network Policies for security
- [ ] Monitoring (Prometheus/Grafana)

## Additional Resources

- [DigitalOcean Kubernetes Docs](https://docs.digitalocean.com/products/kubernetes/)
- [Terraform DigitalOcean Provider](https://registry.terraform.io/providers/digitalocean/digitalocean/latest/docs)
- [Terraform Cloud Docs](https://developer.hashicorp.com/terraform/cloud-docs)