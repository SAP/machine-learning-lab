from setuptools import setup, find_packages


setup(
    name="ml_lab_plugin",
    version="0.0.1",
    description="Plugin for ML Lab's ML Flow component",
    packages=find_packages(),
    # Require MLflow as a dependency of the plugin, so that plugin users can simply install
    # the plugin & then immediately use it with MLflow
    install_requires=["mlflow", "contaxy"],
    entry_points={
        # Define a Tracking Store plugin for tracking URIs with scheme 'ml-lab'
        "mlflow.tracking_store": "ml-lab=ml_lab_plugin.tracking_store:MlLabTrackingStore",
        # Define a ArtifactRepository plugin for artifact URIs with scheme 'ml-lab'
        "mlflow.artifact_repository": "ml-lab=ml_lab_plugin.artifacts:MlLabArtifactRepository",  # pylint: disable=line-too-long
    },
)
