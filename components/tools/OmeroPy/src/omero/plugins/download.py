#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
   download plugin

   Plugin read by omero.cli.Cli during initialization. The method(s)
   defined here will be added to the Cli class for later use.

   Copyright 2007 Glencoe Software, Inc. All rights reserved.
   Use is subject to license terms supplied in LICENSE.txt

"""

import sys
import omero
import re
from omero.cli import BaseControl, CLI
from omero.rtypes import unwrap

HELP = """Download the given file id to the given filename

Examples:

    # Download OriginalFile 2 to local_file
    bin/omero download OriginalFile:2 local_file
    # Download Original File 2 to the stdout
    bin/omero download 2 my_file

    # Download the OriginalFile linked to FileAnnotation 20 to local_file
    bin/omero download FileAnnotation:20 my_file

    # Download the OriginalFile linked to Image 5
    # Works only with single files imported with OMERO 5.0.0 and above
    bin/omero download Image:5 original_image
"""


class DownloadControl(BaseControl):

    def _configure(self, parser):
        parser.add_argument(
            "object", help="Object to download of form <object>:<image_id>. "
            "OriginalFile is assumed if <object>: is omitted.")
        parser.add_argument(
            "filename", help="Local filename to be saved to. '-' for stdout")
        parser.set_defaults(func=self.__call__)
        parser.add_login_arguments()

    def __call__(self, args):
        from omero_model_OriginalFileI import OriginalFileI as OFile

        # Retrieve connection
        client = self.ctx.conn(args)
        file_id = self.get_file_id(client.sf, args.object)
        orig_file = OFile(file_id)
        target_file = str(args.filename)

        try:
            if target_file == "-":
                client.download(orig_file, filehandle=sys.stdout)
                sys.stdout.flush()
            else:
                client.download(orig_file, target_file)
        except omero.ValidationException, ve:
            # Possible, though unlikely after previous check
            self.ctx.die(67, "Unknown ValidationException: %s"
                         % ve.message)

    def get_file_id(self, session, value):

        query = session.getQueryService()
        if ':' not in value:
            try:
                ofile = query.get("OriginalFile", long(value),
                    {'omero.group': '-1'})
                return ofile.id.val
            except ValueError:
                self.ctx.die(601, 'Invalid OriginalFile ID input')
            except omero.ValidationException:
                self.ctx.die(601, 'No OriginalFile with input ID')

        # Assume input is of form OriginalFile:id
        file_id = self.parse_object_id("OriginalFile", value)
        if file_id:
            try:
                ofile = query.get("OriginalFile", file_id)
            except omero.ValidationException:
                self.ctx.die(601, 'No OriginalFile with input ID')
            return ofile.id.val

        # Assume input is of form FileAnnotation:id
        fa_id = self.parse_object_id("FileAnnotation", value)
        if fa_id:
            try:
                fa = query.get("FileAnnotation", fa_id)
            except omero.ValidationException:
                self.ctx.die(601, 'No FileAnnotation with input ID')
            return fa.getFile().id.val

        # Assume input is of form Image:id
        image_id = self.parse_object_id("Image", value)
        params = omero.sys.ParametersI()
        if image_id:
            params.addLong('iid', image_id)
            sql = "select f from Image i" \
                " left outer join i.fileset as fs" \
                " join fs.usedFiles as uf" \
                " join uf.originalFile as f" \
                " where i.id = :iid"
            query_out = query.projection(sql, params)
            if not query_out:
                self.ctx.die(602, 'Input image has no associated Fileset')
            if len(query_out) > 1:
                self.ctx.die(603, 'Input image has more than 1 associated '
                             'file: %s' % len(query_out))
            return unwrap(query_out[0])[0].id.val

        self.ctx.die(601, 'Invalid object input')

    def parse_object_id(self, object_type, value):

        pattern = r'%s:(?P<id>\d+)' % object_type
        pattern = re.compile('^' + pattern + '$')
        m = pattern.match(value)
        if not m:
            return
        return long(m.group('id'))

try:
    register("download", DownloadControl, HELP)
except NameError:
    if __name__ == "__main__":
        cli = CLI()
        cli.register("download", DownloadControl, HELP)
        cli.invoke(sys.argv[1:])
