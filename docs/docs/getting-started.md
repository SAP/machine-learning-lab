# Getting Started in 30 seconds

You just found out about ML Lab and want to try it out? Congrats :tada:, this is officially the fastest way to get you started. To get started, you have two options:

- [Getting Started in 30 seconds](#getting-started-in-30-seconds)
  - [Install ML Lab](#install-ml-lab)
  - [Next steps](#next-steps)

!!! danger "At your own risk!"
    Even so ML Lab is, of course, suuuper stable 😎 it is intended for ML Experimentation and Model Testing, not necessarily for production-critical deployment.

## Install ML Lab

To install ML Lab, all you need is a single machine, preferably Mac or Linux, and a few seconds of your precious time ... ok maybe a few minutes, but it's worth the time :blush:.

!!! info "Wait... there is one requirement"
    ML Lab requires Docker to be installed on your host machine.

Docker is running on your machine? Perfect! Now everything is ready for the big moment :fireworks: Just run this command:

```
docker run --rm --env LAB_ACTION=install -v /var/run/docker.sock:/var/run/docker.sock --env LAB_PORT=8091 lab-service:latest
```

Yeay, that was easy :relieved: The ML Lab install process will automatically download and install everything it needs. This may take a few minutes depending on your internet speed. Grab a coffee :coffee: and take a few minutes to dream about all the exciting things you can do with this tool :unicorn:. After some minutes, is the Web UI finally loading on `http://<HOSTIP>:8091`?

- **Yes:** Congratulations :champagne_glass:, everything is set up and you are ready for [next steps](#next-steps)!
- **No:** Sorry :worried:, there might be an error with the installation. Please check the logs and refer to this [installation guide](../installation/install-lab/) for more details.

## Next steps

ML Lab is ready to go :rocket: Now what?

- [Walkthrough](../walkthrough/lab-walkthrough/): A visual tutorial that helps you explore all of the best functionalities.
- [Full Installation Guide](../installation/install-lab/): A guide that helps you to deploy ML Lab to production, so that everyone on your team can get first-class access.
