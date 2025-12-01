terraform {
  required_providers {
    kubernetes = {
      source = "hashicorp/kubernetes"
    }
  }
}

resource "kubernetes_deployment" "this" {
  metadata {
    name      = var.name
    namespace = var.namespace
    labels = {
      app = var.name
    }
  }

  wait_for_rollout = true

  timeouts {
    create = "20m"
    update = "20m"
  }

  spec {
    replicas = var.replicas

    selector {
      match_labels = {
        app = var.name
      }
    }

    template {
      metadata {
        labels = {
          app = var.name
        }
      }

      spec {

        restart_policy = "Always"

        container {
          image = var.image
          name  = var.name

          dynamic "port" {
            for_each = var.ports
            content {
              container_port = port.value
            }
          }

          dynamic "env" {
            for_each = var.env_vars
            content {
              name  = env.key
              value = env.value
            }
          }

          resources {
            limits = {
              memory = var.memory_limit
              cpu    = var.cpu_limit
            }
            requests = {
              memory = var.memory_request
              cpu    = var.cpu_request
            }
          }

          readiness_probe {
            http_get {
              path = "/actuator/health"
              port = var.ports[0]
            }
            initial_delay_seconds = 10
            period_seconds        = 5
          }

          liveness_probe {
            http_get {
              path = "/actuator/health"
              port = var.ports[0]
            }
            initial_delay_seconds = 20
            period_seconds        = 10
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "this" {
  metadata {
    name      = var.name
    namespace = var.namespace
  }

  spec {
    selector = {
      app = var.name
    }

    dynamic "port" {
      for_each = var.ports
      content {
        port        = port.value
        target_port = port.value
      }
    }

    type = var.service_type
  }
}