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
