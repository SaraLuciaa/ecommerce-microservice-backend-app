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

data "digitalocean_kubernetes_cluster" "dev" {
  name = digitalocean_kubernetes_cluster.dev.name
  depends_on = [digitalocean_kubernetes_cluster.dev]
}

provider "kubernetes" {
  host                   = data.digitalocean_kubernetes_cluster.dev.endpoint
  token                  = data.digitalocean_kubernetes_cluster.dev.kube_config[0].token
  cluster_ca_certificate = base64decode(data.digitalocean_kubernetes_cluster.dev.kube_config[0].cluster_ca_certificate)
  # load_config_file       = false # This attribute is deprecated in newer versions of the provider, but implied by setting host/token
}