import os
import re
import inspect

ITEM_COLUMN = "item"
SCORE_COLUMN = "score"


def resolve_camel_case(name):
    s1 = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', name)
    return re.sub('([a-z0-9])([A-Z])', r'\1_\2', s1).lower()


def truncate_middle(s, n):
    if len(s) <= n:
        # string is already short-enough
        return s
    # half of the size, minus the 3 .'s
    n_2 = int(n) / 2 - 3
    # whatever's left
    n_1 = n - n_2 - 3
    return '{0}...{1}'.format(s[:int(n_1)], s[-int(n_2):])


def md5(file_path):
    import hashlib
    # return hashlib.md5(open(fname, 'rb').read()).hexdigest()
    hash_md5 = hashlib.md5()
    with open(file_path, "rb") as f:
        for chunk in iter(lambda: f.read(4096), b""):
            hash_md5.update(chunk)
    return hash_md5.hexdigest()


def simplify(name):
    return resolve_camel_case(name).strip().replace(" ", "_")


def get_file_name(file_path):
    return os.path.splitext(os.path.basename(file_path))[0]


def overrides(method):
    # actually can't do this because a method is really just a function while inside a class def'n
    #assert(inspect.ismethod(method))

    stack = inspect.stack()
    base_classes = re.search(r'class.+\((.+)\)\s*\:', stack[2][4][0]).group(1)

    # handle multiple inheritance
    base_classes = [s.strip() for s in base_classes.split(',')]
    if not base_classes:
        raise ValueError('overrides decorator: unable to determine base class')

    # stack[0]=overrides, stack[1]=inside class def'n, stack[2]=outside class def'n
    derived_class_locals = stack[2][0].f_locals

    # replace each class name in base_classes with the actual class type
    for i, base_class in enumerate(base_classes):

        if '.' not in base_class:
            base_classes[i] = derived_class_locals[base_class]

        else:
            components = base_class.split('.')

            # obj is either a module or a class
            obj = derived_class_locals[components[0]]

            for c in components[1:]:
                assert(inspect.ismodule(obj) or inspect.isclass(obj))
                obj = getattr(obj, c)

            base_classes[i] = obj

    assert( any( hasattr(cls, method.__name__) for cls in base_classes ) )
    return method
