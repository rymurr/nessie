# -*- coding: utf-8 -*-
import click

from . import NessieClient
from .model import ReferenceSchema


def handle_branch_tag(
    nessie: NessieClient,
    list_references: bool,
    delete_reference: bool,
    branch: str,
    new_branch: str,
    is_branch: bool,
    json: bool,
    force: bool,
    verbose: bool,
) -> str:
    """Perform branch actions."""
    if list_references or (not list_references and not delete_reference and not branch and not new_branch):
        return _handle_list(nessie, json, verbose, is_branch, branch)
    elif delete_reference:
        branch_object = nessie.get_reference(branch)
        getattr(nessie, "delete_{}".format("branch" if is_branch else "tag"))(branch, branch_object.hash_)
    elif branch and not new_branch:
        getattr(nessie, "create_{}".format("branch" if is_branch else "tag"))(branch)
        nessie.create_branch(branch)
    elif branch and new_branch and not force:
        getattr(nessie, "create_{}".format("branch" if is_branch else "tag"))(branch, new_branch)
    else:
        getattr(nessie, "assign_{}".format("branch" if is_branch else "tag"))(branch, new_branch)
    return ""


def _handle_list(nessie: NessieClient, json: bool, verbose: bool, is_branch: bool, branch: str) -> str:
    results = nessie.list_references()
    if json:
        return ReferenceSchema().dumps([i for i in results if i.kind == ("BRANCH" if is_branch else "TAG")], many=True)
    output = ""
    default_branch = nessie.get_default_branch()
    kept_results = [i for i in results if i.kind == ("BRANCH" if is_branch else "TAG")]
    if branch:
        kept_results = [i for i in kept_results if i.name == branch]
    max_width = max(len(i.name) for i in kept_results)
    for x in results:
        next_row = "{}{}{}{}{}\n".format(
            "*".ljust(2) if x.name == default_branch else "  ",
            x.name.ljust(max_width + 1),
            " " if verbose else "",
            x.hash_ if verbose else "",
            " comment" if verbose else "",
        )
        output += click.style(next_row, fg="yellow") if x.name == default_branch else next_row
    return output
