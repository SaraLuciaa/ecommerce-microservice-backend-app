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

  cloud {
    organization = "ingesoffttt"

    workspaces {
      name = "ecommerce-stage"
    }
  }
}

provider "digitalocean" {
  token = var.do_token
}

# Solo CONSULTA el cluster existente
data "digitalocean_kubernetes_cluster" "cluster" {
  name = var.cluster_name
}

provider "kubernetes" {
  host                   = data.digitalocean_kubernetes_cluster.cluster.endpoint
  token                  = data.digitalocean_kubernetes_cluster.cluster.kube_config[0].token
  cluster_ca_certificate = base64decode(data.digitalocean_kubernetes_cluster.cluster.kube_config[0].cluster_ca_certificate)
}
