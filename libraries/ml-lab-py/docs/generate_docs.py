"""
Parses source code to generate API docs in markdown.
"""

import inspect
import os
import re
import sys
from inspect import getdoc, getsourcefile, getsourcelines, getmembers

if sys.version[0] == '2':
    reload(sys)
    sys.setdefaultencoding('utf8')

_RE_BLOCKSTART = re.compile(
    r"(Args:|Arg:|Arguments:|Parameters:|Kwargs:|Attributes:|Returns:|Yields:|Kwargs:|Raises:|Notes:|Note:|Examples:|Example:)",
    re.IGNORECASE)
_RE_ARGSTART = re.compile(r"(\w*?)\s*?\((.*?)\):(.*)", re.IGNORECASE)
_RE_EXCSTART = re.compile(r"(\w*?):(.*)", re.IGNORECASE)

#
# String templates
#

FUNC_TEMPLATE = \
"""-------------------
<span style="float:right;">[[source]]({path})</span>

{section} {header}

```python
{funcdef}
```
{doc}
"""

CLASS_TEMPLATE = \
"""-------------------
<span style="float:right;">[[source]]({path})</span>

{section} {header} class
{doc}
{variables}
{init}
{handlers}
{methods}
"""

MODULE_TEMPLATE = \
"""
**Source:** {path}
{global_vars}
{functions}
{classes}
"""


def get_function_signature(function, owner_class=None, show_module=False):
    isclass = inspect.isclass(function)

    # Get base name.
    name_parts = []
    if show_module:
        name_parts.append(function.__module__)
    if owner_class:
        name_parts.append(owner_class.__name__)
    if hasattr(function, '__name__'):
        name_parts.append(function.__name__)
    else:
        name_parts.append(type(function).__name__)
        name_parts.append('__call__')
        function = function.__call__
    name = '.'.join(name_parts)

    if isclass:
        function = getattr(function, '__init__', None)
    if hasattr(inspect, 'signature'):
        sig = str(inspect.signature(function))
    else:
        try:
            argspec = inspect.getargspec(function)
        except TypeError:
            # handle Py2 classes that don't define __init__
            args = ['self']
        else:
            # Generate the argument list that is separated by colons.
            args = argspec.args[:]
            if argspec.defaults:
                offset = len(args) - len(argspec.defaults)
                for i, default in enumerate(argspec.defaults):
                    args[i + offset] = '{}={!r}'.format(args[i + offset], argspec.defaults[i])
            if argspec.varargs:
                args.append('*' + argspec.varargs)
            if argspec.keywords:
                args.append('**' + argspec.keywords)
        sig = '(' + ', '.join(args) + ')'

    return name + sig


def make_iter(obj):
    """ Makes an iterable
    """
    return obj if hasattr(obj, '__iter__') else [obj]


def order_by_line_nos(objs, line_nos):
    """Orders the set of `objs` by `line_nos`
    """
    ordering = sorted(range(len(line_nos)), key=line_nos.__getitem__)
    return [objs[i] for i in ordering]


def to_md_file(string, filename, out_path="."):
    """Import a module path and create an api doc from it
    Args:
        string (str): string with line breaks to write to file.
        filename (str): filename without the .md
        out_path (str): The output directory
    """
    md_file = "%s.md" % filename
    with open(os.path.join(out_path, md_file), "w") as f:
        f.write(string)
    print("wrote {}.".format(md_file))


def code_snippet(snippet):
    result = '```python\n'
    result += snippet + '\n'
    result += '```\n'
    return result


class MarkdownAPIGenerator(object):
    def __init__(self, src_root, github_link):
        """Initializes the markdown api generator.
        Args:
            src_root: The root folder name containing all the sources.
                Ex: src
            github_link: The base github link. Should include branch name.
                Ex: https://github.com/raghakot/keras-vis/tree/master
                All source links are generated with this prefix.
        """
        self.src_root = src_root
        self.github_link = github_link

    def get_line_no(self, obj):
        """Gets the source line number of this object. None if `obj` code cannot be found.
        """
        try:
            lineno = getsourcelines(obj)[1]
        except:
            # no code found
            lineno = None
        return lineno

    def get_src_path(self, obj, append_base=True):
        """Creates a src path string with line info for use as markdown link.
        """
        path = getsourcefile(obj)
        if not self.src_root in path:
            # this can happen with e.g.
            # inlinefunc-wrapped functions
            if hasattr(obj, "__module__"):
                path = "%s.%s" % (obj.__module__, obj.__name__)
            else:
                path = obj.__name__
            path = path.replace(".", "/")
        print(path)
        pre, post = path.rsplit(self.src_root + "/", 1)

        lineno = self.get_line_no(obj)
        lineno = "" if lineno is None else "#L{}".format(lineno)

        path = self.src_root + "/" + post + lineno
        if append_base:
            path = os.path.join(self.github_link, path)
        return path

    def doc2md(self, func):
        """Parse docstring (parsed with getdoc) according to Google-style
        formatting and convert to markdown. We support the following
        Google style syntax:
        Args, Kwargs:
            argname (type): text
            freeform text
        Returns, Yields:
            retname (type): text
            freeform text
        Raises:
            exceptiontype: text
            freeform text
        Notes, Examples:
            freeform text
        """
        doc = getdoc(func) or ""
        blockindent = 0
        argindent = 1
        out = []

        for line in doc.split("\n"):
            indent = len(line) - len(line.lstrip())
            line = line.lstrip()
            if _RE_BLOCKSTART.match(line):
                # start of a new block
                blockindent = indent
                out.append("\n**{}**\n".format(line))
            elif indent > blockindent:
                if _RE_ARGSTART.match(line):
                    # start of new argument
                    out.append("\n" + " " * blockindent + " - " + _RE_ARGSTART.sub(r"`\1` (\2): \3", line))
                    argindent = indent
                elif _RE_EXCSTART.match(line):
                    # start of an exception-type block
                    out.append("\n" + " " * blockindent + " - " + _RE_EXCSTART.sub(r"`\1`: \2", line))
                    argindent = indent
                elif indent > argindent:
                    out.append("\n" + " " * (blockindent + 2) + line)
                else:
                    out.append("\n" + line)
            else:
                out.append("\n" + line)

        return "".join(out)

    def func2md(self, func, clsname="", depth=3):
        """Takes a function (or method) and documents it.
        Args:
            clsname (str, optional): class name to prepend to funcname.
            depth (int, optional): number of ### to append to function name
        """
        section = "#" * depth
        funcname = func.__name__
        escfuncname = "`%s`" % funcname if funcname.startswith("_") else funcname
        header = "%s%s" % ("%s." % clsname if clsname else "", escfuncname)

        path = self.get_src_path(func)
        doc = self.doc2md(func)

        funcdef = get_function_signature(func)
        funcdef = funcdef.replace("=", " = ")
        funcdef = funcdef.replace(":", ": ")

        # split the function definition if it is too long
        lmax = 70
        if len(funcdef) > lmax:
            import re
            param_part = re.findall("\((.*?)\)", funcdef)
            if len(param_part) > 0:
                param_part = param_part[0]
            param_str = ""
            params = param_part.split(", ")
            for i, param in enumerate(params):
                if i is not len(params) - 1:
                    param_str += "    " + str(param) + ",\n"
                else:
                    param_str += "    " + str(param) + "\n"

            # wrap in the args list
            first_part = funcdef.split("(", 1)[0]
            last_part = funcdef.split(")", 1)[1]

            funcdef = first_part + "(\n"
            funcdef += param_str
            funcdef += ")" + last_part

        # build the signature
        string = FUNC_TEMPLATE.format(section=section,
                                      header=header,
                                      funcdef=funcdef,
                                      path=path,
                                      doc=doc if doc else "*No documentation found.*")
        return string

    def class2md(self, cls, depth=2):
        """Takes a class and creates markdown text to document its methods and variables.
        """

        section = "#" * depth
        subsection = "#" * (depth + 2)
        clsname = cls.__name__
        modname = cls.__module__
        header = clsname
        path = self.get_src_path(cls)
        doc = self.doc2md(cls)

        try:
            init = self.func2md(cls.__init__, clsname=clsname)
        except (ValueError, TypeError):
            # this happens if __init__ is outside the repo
            init = ""

        variables = []
        for name, obj in getmembers(cls, lambda a: not (inspect.isroutine(a) or inspect.ismethod(a))):
            if not name.startswith("_") and type(obj) == property:
                comments = self.doc2md(obj) or inspect.getcomments(obj)
                comments = "\n %s" % comments if comments else ""
                variables.append("\n%s %s.%s%s\n" % (subsection, clsname, name, comments))

        handlers = []
        for name, obj in getmembers(cls, inspect.ismethoddescriptor):
            if not name.startswith("_") and hasattr(obj, "__module__") and obj.__module__ == modname:
                handlers.append("\n%s %s.%s\n *Handler*" % (subsection, clsname, name))

        methods = []
        for name, obj in getmembers(cls, inspect.isfunction):
            print(name)
            if not name.startswith("_") and hasattr(obj,
                                                    "__module__") and obj.__module__ == modname and name not in handlers:
                methods.append(self.func2md(obj, clsname=clsname, depth=depth + 1))

        string = CLASS_TEMPLATE.format(section=section,
                                       header=header,
                                       path=path,
                                       doc=doc if doc else "",
                                       init=init,
                                       variables="".join(variables),
                                       handlers="".join(handlers),
                                       methods="".join(methods))
        return string

    def module2md(self, module):
        """Takes an imported module object and create a Markdown string containing functions and classes.
        """
        modname = module.__name__
        path = self.get_src_path(module, append_base=False)
        path = "[{}]({})".format(path, os.path.join(self.github_link, path))
        found = []

        classes = []
        line_nos = []
        for name, obj in getmembers(module, inspect.isclass):
            # handle classes
            found.append(name)
            if not name.startswith("_") and hasattr(obj, "__module__") and obj.__module__ == modname:
                classes.append(self.class2md(obj))
                line_nos.append(self.get_line_no(obj) or 0)
        classes = order_by_line_nos(classes, line_nos)

        functions = []
        line_nos = []
        for name, obj in getmembers(module, inspect.isfunction):
            # handle functions
            found.append(name)
            if not name.startswith("_") and hasattr(obj, "__module__") and obj.__module__ == modname:
                functions.append(self.func2md(obj))
                line_nos.append(self.get_line_no(obj) or 0)
        functions = order_by_line_nos(functions, line_nos)

        variables = []
        line_nos = []
        for name, obj in module.__dict__.items():
            if not name.startswith("_") and name not in found:
                if hasattr(obj, "__module__") and obj.__module__ != modname:
                    continue
                if hasattr(obj, "__name__") and not obj.__name__.startswith(modname):
                    continue
                comments = inspect.getcomments(obj)
                comments = ": %s" % comments if comments else ""
                variables.append("- **%s**%s" % (name, comments))
                line_nos.append(self.get_line_no(obj) or 0)

        variables = order_by_line_nos(variables, line_nos)
        if variables:
            new_list = ["**Global Variables**", "---------------"]
            new_list.extend(variables)
            variables = new_list

        string = MODULE_TEMPLATE.format(path=path,
                                        global_vars="\n".join(variables) if variables else "",
                                        functions="\n".join(functions) if functions else "",
                                        classes="".join(classes) if classes else "")
        return string