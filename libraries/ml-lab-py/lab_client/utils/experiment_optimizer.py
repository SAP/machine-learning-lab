"""Collection of hyperparameter optimizers."""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import numbers

from lab_client.commons import text_utils
from lab_client.handler.experiment_handler import ExperimentGroup
from lab_client.utils import experiment_utils


class ParamIteratorOptimizer(ExperimentGroup):
    """
    Simple Parameter Optimizer based on an parameter space iterator.

    # Arguments
        base_exp (#Experiment): Base experiment that will be cloned for this experiment group.
        group_key (string): Group key shared between all experiments in the same group.
            If `None`, the name of the base experiment will be used (optional).
    """

    def __init__(self, base_exp, group_key: str = None,
                 context_symbol_table: dict = None):
        super(ParamIteratorOptimizer, self).__init__(base_exp, group_key, context_symbol_table=context_symbol_table)

    def run(self, exp_function: list or function, params: dict = None, artifacts: dict = None,
            param_space_iter=None, max_evals: int = None) -> list:
        """
        Runs the given experiment function or list of functions with various parameter configuration

        # Arguments
            exp_function (function or list): Method that implements the experiment.
            params (dict): Dictionary that contains the configuration (e.g. hyperparameters) for the experiment (optional).
            artifacts (dict): Dictionary that contains artifacts (any kind of python object) required for the experiment (optional).
            param_space_iter (iterator): An iterator of configuration dicts.
            max_evals (int): Maximum number of evaluations (optional).

        # Returns
        List of finished experiments
        """
        evals = 0
        for selected_params in param_space_iter:
            evals += 1
            exp = self.create_exp(name_suffix=text_utils.simplify_dict_to_str(selected_params))
            params.update(selected_params)
            exp.run_exp(exp_function, params, artifacts)
            if max_evals and evals >= max_evals:
                # Stop evaluation
                break
        return self.experiments()


class GridSearchOptimizer(ParamIteratorOptimizer):
    """
    Simple Grid Search Optimizer based on Sklearn's ParameterGrid:
    https://scikit-learn.org/stable/modules/generated/sklearn.model_selection.ParameterGrid.html

    # Arguments
        base_exp (#Experiment): Base experiment that will be cloned for this experiment group.
        group_key (string): Group key shared between all experiments in the same group.
            If `None`, the name of the base experiment will be used (optional).
    """

    def __init__(self, base_exp, group_key: str = None):
        super(GridSearchOptimizer, self).__init__(base_exp, group_key,
                                                  context_symbol_table=experiment_utils.get_caller_symbol_table())

    def run(self, exp_function: list or function, params: dict = None, artifacts: dict = None,
            param_space=None, max_evals: int = None) -> list:
        """
        Runs the given experiment function or list of functions with various parameter configuration

        # Arguments
            exp_function (function or list): Method that implements the experiment.
            params (dict): Dictionary that contains the configuration (e.g. hyperparameters) for the experiment (optional).
            artifacts (dict): Dictionary that contains artifacts (any kind of python object) required for the experiment (optional).
            param_space (iterator): A parameter space suited for sklearn `ParameterGrid`.
            max_evals (int): Maximum number of evaluations (optional).

        # Returns
        List of finished experiments
        """
        from sklearn.model_selection import ParameterGrid
        return super(GridSearchOptimizer, self).run(exp_function, params, artifacts, ParameterGrid(param_space),
                                                    max_evals)


class RandomSearchOptimizer(ParamIteratorOptimizer):
    """
    Simple Random Search Optimizer based on Sklearn's ParameterSampler:
    https://scikit-learn.org/stable/modules/generated/sklearn.model_selection.ParameterSampler.html

    # Arguments
        base_exp (#Experiment): Base experiment that will be cloned for this experiment group.
        group_key (string): Group key shared between all experiments in the same group.
            If `None`, the name of the base experiment will be used (optional).
    """

    def __init__(self, base_exp, group_key: str = None):
        super(RandomSearchOptimizer, self).__init__(base_exp, group_key,
                                                    context_symbol_table=experiment_utils.get_caller_symbol_table())

    def run(self, exp_function: list or function, params: dict = None, artifacts: dict = None,
            param_space=None, max_evals: int = None) -> list:
        """
        Runs the given experiment function or list of functions with various parameter configuration

        # Arguments
            exp_function (function or list): Method that implements the experiment.
            params (dict): Dictionary that contains the configuration (e.g. hyperparameters) for the experiment (optional).
            artifacts (dict): Dictionary that contains artifacts (any kind of python object) required for the experiment (optional).
            param_space (iterator): A parameter space suited for sklearn `ParameterGrid`.
            max_evals (int): Maximum number of evaluations (optional).

        # Returns
        List of finished experiments
        """
        from sklearn.model_selection import ParameterSampler
        return super(RandomSearchOptimizer, self).run(exp_function, params, artifacts,
                                                      ParameterSampler(param_space, max_evals), max_evals)


class HyperoptOptimizer(ExperimentGroup):
    """
    Hyperparameter Optimizer based on Hyperopt: https://github.com/hyperopt/hyperopt

    # Arguments
        base_exp (#Experiment): Base experiment that will be cloned for this experiment group.
        group_key (string): Group key shared between all experiments in the same group.
            If `None`, the name of the base experiment will be used (optional).
        maximize_score (bool): If `True`, the objective is set to maximize the returned metric. Default: True.
    """

    def __init__(self, base_exp, group_key: str = None, maximize_score: bool = True, **kwargs):
        super(HyperoptOptimizer, self).__init__(base_exp, group_key,
                                                context_symbol_table=experiment_utils.get_caller_symbol_table())
        self.maximize_score = maximize_score
        self.fmin_args = kwargs
        self.hyperopt_trials = None
        self.hyperopt_result = None

    def run(self, exp_function: list or function, params: dict = None, artifacts: dict = None, param_space=None,
            max_evals=10) -> list:
        """
        Runs the given experiment function or list of functions with various parameter configuration

        # Arguments
            exp_function (function or list): Method that implements the experiment.
            params (dict): Dictionary that contains the configuration (e.g. hyperparameters) for the experiment (optional).
            artifacts (dict): Dictionary that contains artifacts (any kind of python object) required for the experiment (optional).
            param_space (iterator): Paramter Space suited for Hyperopt Library.
            max_evals (int): Maximum number of evaluations. Default: 10.

        # Returns
        List of finished experiments
        """

        def hyperopt_objective(param_selection):
            exp = self.create_exp(name_suffix=text_utils.simplify_dict_to_str(param_selection))
            params.update(param_selection)
            exp.run_exp(exp_function, params, artifacts)
            score = exp.exp_metadata.result
            if not score or not isinstance(score, numbers.Number):
                exp.log.info("Provided exp_function did not return a numeric score/result.")
                return None
            if self.maximize_score:
                # hyperopt only supports min
                score = -score
            return score

        from hyperopt import fmin, Trials, tpe

        self.hyperopt_trials = Trials()
        hyperopt_algo = tpe.suggest
        if self.fmin_args:
            if "algo" in self.fmin_args:
                hyperopt_algo = self.fmin_args["algo"]

            if "trials" in self.fmin_args:
                self.hyperopt_trials = self.fmin_args["trials"]

        self.hyperopt_result = fmin(hyperopt_objective, param_space, max_evals=max_evals, algo=hyperopt_algo,
                                    trials=self.hyperopt_trials, **self.fmin_args)
        return self.experiments()

    def plot_results(self):
        """
        Visualize Hyperparameter Optimization Results
        """
        super(HyperoptOptimizer, self).plot_results()

        import hyperopt.plotting
        hyperopt.plotting.main_plot_history(self.hyperopt_trials)
        hyperopt.plotting.main_plot_histogram(self.hyperopt_trials)
        try:
            hyperopt.plotting.main_plot_vars(self.hyperopt_trials)
        except:
            pass


class SkoptOptimizer(ExperimentGroup):
    """
    Hyperparameter Optimizer based on Skopt: https://github.com/scikit-optimize/scikit-optimize

    You can provide additional named paramters that are available in the `<algo>_minimize` function.

    # Arguments
        base_exp (#Experiment): Base experiment that will be cloned for this experiment group.
        group_key (string): Group key shared between all experiments in the same group.
            If `None`, the name of the base experiment will be used (optional).
        maximize_score (bool): If `True`, the objective is set to maximize the returned metric. Default: True.
        algo (str): Algorithm to use for optimization. Possible values: gp, gbrt, forest, dummy.  Default: gp.
    """

    def __init__(self, base_exp, group_key: str = None, maximize_score: bool = True, algo: str = "gp", **kwargs):
        super(SkoptOptimizer, self).__init__(base_exp, group_key,
                                             context_symbol_table=experiment_utils.get_caller_symbol_table())
        self.maximize_score = maximize_score
        self.skopt_args = kwargs
        self.skopt_algo = algo

    def run(self, exp_function: list or function, params: dict = None, artifacts: dict = None, param_space=None,
            max_evals=10) -> list:
        """
        Runs the given experiment function or list of functions with various parameter configuration

        # Arguments
            exp_function (function or list): Method that implements the experiment.
            params (dict): Dictionary that contains the configuration (e.g. hyperparameters) for the experiment (optional).
            artifacts (dict): Dictionary that contains artifacts (any kind of python object) required for the experiment (optional).
            param_space (iterator): Parameter Space suited for Skopt Library.
            max_evals (int): Maximum number of evaluations. Default: 10.

        # Returns
        List of finished experiments
        """
        from skopt.utils import use_named_args

        @use_named_args(param_space)
        def skopt_objective(**param_selection):
            exp = self.create_exp(name_suffix=text_utils.simplify_dict_to_str(param_selection))
            params.update(param_selection)
            exp.run_exp(exp_function, params, artifacts)
            score = exp.exp_metadata.result
            if not score or not isinstance(score, numbers.Number):
                exp.log.info("Provided exp_function did not return a numeric score/result.")
                return None
            if self.maximize_score:
                # hyperopt only supports min
                score = -score
            return score

        if self.skopt_algo == "gp":
            from skopt import gp_minimize
            self.skopt_result = gp_minimize(skopt_objective, param_space, n_calls=max_evals, **self.skopt_args)
        elif self.skopt_algo == "gbrt":
            from skopt import gbrt_minimize
            self.skopt_result = gbrt_minimize(skopt_objective, param_space, n_calls=max_evals, **self.skopt_args)
        elif self.skopt_algo == "forest":
            from skopt import forest_minimize
            self.skopt_result = forest_minimize(skopt_objective, param_space, n_calls=max_evals, **self.skopt_args)
        elif self.skopt_algo == "dummy":
            from skopt import dummy_minimize
            self.skopt_result = dummy_minimize(skopt_objective, param_space, n_calls=max_evals, **self.skopt_args)
        else:
            self.log.warning("The selected algorithm is unknown: " + self.skopt_algo)
        return self.experiments()

    def plot_results(self):
        """
        Visualize Hyperparameter Optimization Results
        """
        super(SkoptOptimizer, self).plot_results()

        import skopt.plots
        try:
            skopt.plots.plot_convergence(self.skopt_result)
        except:
            pass

        try:
            skopt.plots.plot_objective(self.skopt_result)
        except:
            pass

        try:
            skopt.plots.plot_evaluations(self.skopt_result)
        except:
            pass

        try:
            from skopt import expected_minimum
            print(expected_minimum(self.skopt_result))
        except:
            pass


class OptunaOptimizer(ExperimentGroup):
    """
    Hyperparameter Optimizer based on Optuna: https://github.com/pfnet/optuna

    You can provide additional named paramters that are available in the `optuna.create_study` function.

    # Arguments
        base_exp (#Experiment): Base experiment that will be cloned for this experiment group.
        group_key (string): Group key shared between all experiments in the same group.
            If `None`, the name of the base experiment will be used (optional).
        maximize_score (bool): If `True`, the objective is set to maximize the returned metric. Default: True.
    """

    def __init__(self, base_exp, group_key: str = None, maximize_score: bool = True, **kwargs):
        super(OptunaOptimizer, self).__init__(base_exp, group_key,
                                              context_symbol_table=experiment_utils.get_caller_symbol_table())
        self.maximize_score = maximize_score
        self.optuna_args = kwargs
        self.optuna_study = None

    def run(self, exp_function: list or function, params: dict = None, artifacts: dict = None,
            param_selection_func=None, max_evals=10) -> list:
        """
        Runs the given experiment function or list of functions with various parameter configuration

        # Arguments
            exp_function (function or list): Method that implements the experiment.
            params (dict): Dictionary that contains the configuration (e.g. hyperparameters) for the experiment (optional).
            artifacts (dict): Dictionary that contains artifacts (any kind of python object) required for the experiment (optional).
            param_selection_func (function): An iterator of configuration dicts.
            max_evals (int): Maximum number of evaluations. Default: 10.

        # Returns
        List of finished experiments
        """

        def optuna_objective(trial):
            param_selection = param_selection_func(trial)
            exp = self.create_exp(name_suffix=text_utils.simplify_dict_to_str(param_selection))
            params.update(param_selection)
            exp.run_exp(exp_function, params, artifacts)
            score = exp.exp_metadata.result
            if not score or not isinstance(score, numbers.Number):
                exp.log.info("Provided exp_function did not return a numeric score/result.")
                return None
            if self.maximize_score:
                # hyperopt only supports min
                score = -score
            return score

        import optuna
        self.optuna_study = optuna.create_study(**self.optuna_args)
        self.optuna_study.optimize(optuna_objective, n_jobs=1, n_trials=max_evals)
        return self.experiments()

    def plot_results(self):
        """
        Visualize Hyperparameter Optimization Results
        """
        super(OptunaOptimizer, self).plot_results()

        try:
            import optuna.visualization
            optuna.visualization.plot_intermediate_values(self.optuna_study)
        except:
            pass
