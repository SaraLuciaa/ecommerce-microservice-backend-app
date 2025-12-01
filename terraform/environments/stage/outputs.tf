output "cluster_id" {
  description = "ID of the DigitalOcean Kubernetes cluster"
  value       = digitalocean_kubernetes_cluster.stage.id
}

output "cluster_name" {
  description = "Name of the Kubernetes cluster"
  value       = digitalocean_kubernetes_cluster.stage.name
}

output "cluster_endpoint" {
  description = "Endpoint of the Kubernetes cluster"
  value       = digitalocean_kubernetes_cluster.stage.endpoint
}

output "cluster_region" {
  description = "Region where the cluster is deployed"
  value       = digitalocean_kubernetes_cluster.stage.region
}

output "cluster_version" {
  description = "Kubernetes version of the cluster"
  value       = digitalocean_kubernetes_cluster.stage.version
}

output "cluster_status" {
  description = "Status of the cluster"
  value       = digitalocean_kubernetes_cluster.stage.status
}

output "api_gateway_service" {
  description = "API Gateway LoadBalancer endpoint (available after deployment)"
  value       = "Check with: kubectl get svc api-gateway-container -n stage"
}

output "kubeconfig_command" {
  description = "Command to download kubeconfig locally (optional)"
  value       = "doctl kubernetes cluster kubeconfig save ${digitalocean_kubernetes_cluster.stage.id}"
}
