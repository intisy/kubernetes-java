# Kubernetes Java

Archives containing JAR files are available as [releases](https://github.com/intisy/kubernetes-java/releases).

## What is kubernetes-java?

Kubernetes-java provides a standalone Kubernetes cluster for Java using Minikube.

## Usage in private projects

 * Maven (inside the `pom.xml` file)
```xml
  <repository>
      <id>github</id>
      <url>https://maven.pkg.github.com/intisy/kubernetes-java</url>
      <snapshots><enabled>true</enabled></snapshots>
  </repository>
  <dependency>
      <groupId>io.github.intisy</groupId>
      <artifactId>kubernetes-java</artifactId>
      <version>1.0.9</version>
  </dependency>
```

 * Maven (inside the `settings.xml` file)
```xml
  <servers>
      <server>
          <id>github</id>
          <username>your-username</username>
          <password>your-access-token</password>
      </server>
  </servers>
```

 * Gradle (inside the `build.gradle.kts` or `build.gradle` file)
```groovy
  repositories {
      maven {
          url "https://maven.pkg.github.com/intisy/kubernetes-java"
          credentials {
              username = "<your-username>"
              password = "<your-access-token>"
          }
      }
  }
  dependencies {
      implementation 'io.github.intisy:kubernetes-java:1.0.9'
  }
```

## Usage in public projects

 * Gradle (inside the `build.gradle.kts` or `build.gradle` file)
```groovy
  plugins {
      id "io.github.intisy.github-gradle" version "1.3.7"
  }
  dependencies {
      githubImplementation "intisy:kubernetes-java:1.0.9"
  }
```

Once you have it installed you can use it like so:

```
KubernetesProvider kubernetesProvider = KubernetesProvider.get();
kubernetesProvider.ensureInstalled();
kubernetesProvider.start();
KubernetesClient kubernetesClient = kubernetesProvider.getClient();
```

Currently supported setups:
  * [x] - Linux root
  * [x] - Linux rootless
  * [x] - Windows Administrator 
  * [x] - Windows non-Administrator 
  * [x] - macOS root 
  * [x] - macOS rootless

Connecting to an existing cluster from its kubeconfig, instead of starting a local one:

```
KubeConfig config = KubeConfig.parse(Paths.get(".kube/config"));
KubernetesClient client = KubernetesClient.fromKubeConfig(config);
```

`KubeConfig` resolves the current context and reads the server URL, CA, client certificate and
client key, accepting both the inline base64 form and file paths. SEC1 `EC PRIVATE KEY` material is
supported, which is what `talosctl` writes.

A client that cannot build its TLS context, or is handed a CA it cannot use, now throws rather than
continuing without verification. Accepting an unverified server is possible but must be asked for:

```
KubernetesClient.builder().withApiServer(url).withInsecureTrustAll(true).build();
```

Applying manifests, waiting on readiness, and running a command in a pod:

```
client.applyYaml(yaml);                                     // server-side apply, multi-document
client.waitFor("apps/v1", "Deployment", ns, name,
               Conditions::isRolloutComplete, 600000L);     // or Conditions::isReady
ExecResult result = client.exec(ns, pod, "cat", "/data/file");
```

`applyYaml` resolves each document's resource path through the discovery API, so custom resources
work without the library knowing about them. `waitFor` polls conditions and retries transient
failures until its deadline. `exec` throws when the command exits non-zero, and throws separately
when a session ends before the command reported its exit status, so a truncated read is never
mistaken for a successful one.

## License

[![Apache License 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
