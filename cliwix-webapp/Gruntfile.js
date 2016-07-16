var jsApp = [
    'src/main/javascript/app.js',
    'src/main/javascript/main_controller.js',
    'src/main/javascript/login_controller.js',
    'src/main/javascript/export_controller.js',
    'src/main/javascript/import_controller.js',
    'src/main/javascript/settings_controller.js',
    'src/main/javascript/services.js',
    'src/main/javascript/directives.js',
    'src/main/javascript/utils.js',
    'src/main/javascript/messages.js'
];

var jsVendor = [
    'src/main/javascript/ie_lt9_fixes.js',
    'node_modules/angular/angular.min.js',
    'node_modules/angular-route/angular-route.min.js',
    'node_modules/angular-sanitize/angular-sanitize.min.js',
    'node_modules/angular-translate/dist/angular-translate.min.js',
    'node_modules/ng-file-upload/dist/ng-file-upload-shim.min.js',
    'node_modules/ng-file-upload/dist/ng-file-upload.min.js',
    'node_modules/angular-bootstrap/ui-bootstrap-tpls.min.js'
];

var cssVendor = [
    'node_modules/bootstrap/dist/css/bootstrap.min.css',
    'node_modules/bootstrap/dist/css/bootstrap-theme.min.css',
    'node_modules/components-font-awesome/css/font-awesome.min.css'
];

module.exports = function (grunt) {

    grunt
        .initConfig({
            pkg: grunt.file.readJSON('package.json'),

            clean: [ 'src/main/webapp/js', 'src/main/webapp/css' ],

            jshint: {
                src: [ 'Gruntfile.js', 'src/main/javascript/*.js' ]
            },

            sass: {
                dev: {
                    options: {
                        style: 'expanded'
                    },
                    files: {
                        'src/main/webapp/css/app.css': 'src/main/sass/app.scss'
                    }
                },
                dist: {
                    options: {
                        style: 'compressed'
                    },
                    files: {
                        'src/main/webapp/css/app.css': 'src/main/sass/app.scss'
                    }
                }
            },

            concat: {
                jsVendor: {
                    src: jsVendor,
                    dest: 'src/main/webapp/js/vendor.js'
                },
                cssVendor: {
                    src: cssVendor,
                    dest: 'src/main/webapp/css/vendor.css'
                },
                jsApp: {
                    src: jsApp,
                    dest: 'src/main/webapp/js/app.js'
                }
            },

            ngmin: {
                jsApp: {
                    src: 'src/main/webapp/js/app.js',
                    dest: 'src/main/webapp/js/app.js'
                }
            },

            uglify: {
                jsApp: {
                    files: {
                        'src/main/webapp/js/app.js': 'src/main/webapp/js/app.js'
                    }
                }
            },

            copy: {
                fonts: {
                    expand: true,
                    cwd: 'node_modules/components-font-awesome/fonts/',
                    src: '**',
                    dest: 'src/main/webapp/fonts/'
                }
            },

            rev: {
                files: {
                    src: ['src/main/webapp/js/*.js', 'src/main/webapp/css/*.css' ]
                }
            },

            fileblocks: {
                options: {
                    rebuild: true,
                    templates: {
                        'js': '<script type="text/javascript" charset="UTF-8"  src="${file.substring(16)}"></script>',
                        'css': '<link href="${file.substring(16)}" rel="stylesheet" />'
                    }
                },
                'generated_scripts': {
                    src: 'src/main/webapp/index.html',
                    blocks: {
                        'vendor_js': { src: 'src/main/webapp/js/*vendor.js' },
                        'vendor_css': { src: 'src/main/webapp/css/*vendor.css' },
                        'app_js': { src: 'src/main/webapp/js/*app.js' },
                        'app_css': { src: 'src/main/webapp/css/*app.css'  }
                    }
                }
            },

            connect: {
                options: {
                    port: 9000,
                    hostname: 'localhost'
                    //livereload: true
                },
                livereload: {
                    options: {
                        open: true,
                        base: [ 'src/main/webapp/' ]
                    }
                }
            },

            watch: {
                options: {
                    livereload: true
                },
                html: {
                    files: [ 'src/main/webapp/*.html',
                        'src/main/webapp/templates/*.html']
                },
                css: {
                    files: [ 'src/main/sass/*.scss' ],
                    tasks: [ 'sass:dev', 'fileblocks:generated_scripts' ]
                },
                js: {
                    files: [ 'src/main/javascript/*.js' ],
                    tasks: [ 'jshint', 'concat:jsApp', 'fileblocks:generated_scripts' ]
                }
            }

        });

    grunt.loadNpmTasks('grunt-contrib-jshint');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-contrib-connect');
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-sass');
    grunt.loadNpmTasks('grunt-contrib-concat');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-rev');
    grunt.loadNpmTasks('grunt-file-blocks');
    grunt.loadNpmTasks('grunt-ngmin');

    grunt.registerTask('server', function (target) {
        grunt.task.run(['clean', 'jshint', 'sass:dev', 'concat:jsVendor', 'concat:cssVendor', 'concat:jsApp', 'copy:fonts', 'fileblocks:generated_scripts', 'connect:livereload', 'watch']);
    });

    // Default task
    grunt.registerTask('default', [ 'clean', 'jshint', 'sass:dev', 'concat:jsVendor', 'concat:cssVendor', 'concat:jsApp', 'copy:fonts', 'fileblocks:generated_scripts'  ]);

    //Distribution task
    grunt.registerTask('dist', [  'clean', 'jshint', 'sass:dist', 'concat:jsVendor', 'concat:cssVendor', 'concat:jsApp', 'ngmin:jsApp', 'uglify:jsApp', 'copy:fonts', 'rev', 'fileblocks:generated_scripts' ]);
};
