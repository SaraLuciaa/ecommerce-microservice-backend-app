terraform {
  required_version = ">= 1.0"
  required_providers {
    digitalocean = {
      source  = "digitalocean/digitalocean"
      version = "~> 2.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.0"
    }
  }
}

provider "digitalocean" {
  token = var.do_token
}

# Removed data source to avoid plan-time dependency issues
# data "digitalocean_kubernetes_cluster" "dev" { ... }

provider "kubernetes" {
  host                   = digitalocean_kubernetes_cluster.dev.endpoint
  token                  = digitalocean_kubernetes_cluster.dev.kube_config[0].token
  cluster_ca_certificate = base64decode(digitalocean_kubernetes_cluster.dev.kube_config[0].cluster_ca_certificate)
}