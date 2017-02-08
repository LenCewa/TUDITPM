'use strict';
// gruntfile.js

/**
 * Grunt task runner file.
 * 
 * @author       Tobias Mahncke <tobias.mahncke@stud.tu-darmstadt.de>
 * @author       Yannick Pferr <yannick.pferr@stud.tu-darmstadt.de>
 * @version      3.1
 */
module.exports = function(grunt) {
	grunt.initConfig({
		// JS TASKS ================================================================
		// check all js files for errors
		jshint: {
			options: {
				curly: true,
				eqeqeq: true,
				browser: true,
				node: true,
				strict: true,
				latedef: true,
				globals: {
					'$': false,
					'io': false,
					'Cookies': false,
					'Level': false,
					'showAlert': false,
					'localData': false,
					'd3': false,
					'topojson': false,
					'confirm': false
				}
			},
			all: ['public/**/*.js', '!public/libs/**/*.js', '!public/dist/js/*.js', 'app/**/*.js', 'Gruntfile.js']
		},

		jscs: {
			src: ['public/**/*.js', '!public/libs/**/*.js', '!public/dist/js/*.js', 'app/**/*.js', 'Gruntfile.js'],
			options: {
				requireCurlyBraces: ['if', 'else', 'for', 'while', 'do', 'try', 'catch', 'case', 'default'],
				requireSpaceAfterKeywords: ['if', 'else', 'for', 'while', 'do', 'switch', 'return', 'try', 'catch'],
				validateIndentation: '\t',
				disallowMixedSpacesAndTabs: true
			}
		},

		// take the processed style.css file and minify
		cssmin: {
			build: {
				files: {
					'public/dist/css/style.min.css': ['public/css/*.css', '!public/libs/**/*.*']
				}
			}
		},

		// watch css and js files and process the above tasks
		watch: {
			css: {
				files: ['public/css/*.css', '!public/libs/**/*.*'],
				tasks: ['cssmin'],
				options: {
					livereload: true
				}
			},
			js: {
				files: ['public/**/*.js', '!public/libs/**/*.js', '!public/dist/js/*.js'],
				tasks: ['jshint'],
				options: {
					livereload: true
				}
			}
		},

		env: {
			dev: {
				NODE_ENV: 'dev'
			},
			build: {
				NODE_ENV: 'prod'
			}
		},

		// watch our node server for changes
		nodemon: {
			dev: {
				script: 'server.js',
				options: {
					watch: ['app/**/*.js', 'server.js']
				}
			}
		},

		// run watch and nodemon at the same time
		concurrent: {
			options: {
				logConcurrentOutput: true
			},
			tasks: ['nodemon', 'watch']
		},

		// TEST TASKS ==============================================================
		// run Mocha Tests
		mochaTest: {
			backend: {
				src: ['test/**/*.js', '!test/BackTestHelper.js'],
			},
			options: {
				run: true
			}
		}
	});

	// Load all needed grunt dependencies
	grunt.loadNpmTasks('grunt-jscs');
	grunt.loadNpmTasks('grunt-contrib-jshint');
	grunt.loadNpmTasks('grunt-contrib-cssmin');
	grunt.loadNpmTasks('grunt-contrib-watch');
	grunt.loadNpmTasks('grunt-concurrent');
	grunt.loadNpmTasks('grunt-nodemon');
	grunt.loadNpmTasks('grunt-mocha-test');
	grunt.loadNpmTasks('grunt-env');

	// Task to run the server
	grunt.registerTask('default', ['env:dev', 'jshint', 'jscs', 'cssmin', 'concurrent']);
	// Test task
	grunt.registerTask('test', ['jshint', 'jscs', 'mochaTest']);
};