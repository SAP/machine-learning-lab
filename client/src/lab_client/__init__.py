from . import _about
from .environments.base import Environment

# define the version before the other imports since these need it
__version__ = _about.__version__
