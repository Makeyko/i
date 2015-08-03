'use strict';

angular.module('dashboardJsApp').factory('PrintTemplate', function($sce) {
  var PrintTemplate = function() {
    this.task = undefined;
    this.form = undefined;
    this.showPrintModal = false;
  };

  PrintTemplate.prototype.findPrintTemplate = function (form, sCustomFieldID) {
    sCustomFieldID = ((sCustomFieldID!==null && sCustomFieldID!==undefined && sCustomFieldID!=='-') ? sCustomFieldID : 'sBody');
    //$('.aPatternPrint').val();
    var printTemplateResult = form.filter(function (item) {
      return item.id === sCustomFieldID;
    });
    return printTemplateResult.length !== 0 ? printTemplateResult[0].value : "";
  };
  
  /*
  PrintTemplate.prototype.containsPrintTemplate = function () {
    return this.form && this.findPrintTemplate(this.form) !== "";
  };
  */

  //PrintTemplate.prototype.containsPrintTemplate = function (sCustomFieldID) {
  PrintTemplate.prototype.containsPrintTemplate = function () {
    if(!this.form){
        return false;
    }
    var printTemplateResult = this.form.filter(function (item) {
      return item.id.indexOf("sBody")>=0;
    });
    /*if(sCustomFieldID === null || sCustomFieldID === undefined){
      sCustomFieldID = $('.aPatternPrint').val();
    }
    return this.form && this.findPrintTemplate(this.form, sCustomFieldID) !== "";
    */
    return printTemplateResult.length > 0 && printTemplateResult[0].value !== "";
  };

  PrintTemplate.prototype.aPatternPrint = function () {
    var a=[];
    this.form.forEach(function (item, i) {
      if(item.id.indexOf('sBody') >= 0 && item.value !== "" ){
          a=a.concat([{sID:item.id,sLabel:item.name}])
      }
    });    
    if(a.length===0){
        a=a.concat([{sID:"-".id,sLabel:"-"}])
    }
    return a;
  };


  PrintTemplate.prototype.processPrintTemplate = function (form, printTemplate, reg, fieldGetter) {
    var _printTemplate = printTemplate;
    var templates = [], ids = [], found;
    while (found = reg.exec(_printTemplate)) {
      templates.push(found[1]);
      ids.push(found[2]);
    }
    if (templates.length > 0 && ids.length > 0) {
      templates.forEach(function (templateID, i) {
        var id = ids[i];
        if (id) {
          var item = form.filter(function (item) {
            return item.id === id;
          })[0];
          if (item) {
            _printTemplate = _printTemplate.replace(templateID, fieldGetter(item));
          }
        }
      });
    }
    return _printTemplate;
  };

  PrintTemplate.prototype.getPrintTemplate = function () {//sCustomFieldID
    if (!this.form) {
      return "";
    } else {
        
      /*if(sCustomFieldID === null || sCustomFieldID === undefined){
        sCustomFieldID = $('.aPatternPrint').val();
      }*/
      var sCustomFieldID = $('.aPatternPrint').val();
      if(sCustomFieldID === null || sCustomFieldID === undefined || sCustomFieldID === "" || sCustomFieldID === "-"){
          alert("Не обран шаблон для друку!");
          return;
      }
        
      var printTemplate = this.findPrintTemplate(this.form, sCustomFieldID);
      printTemplate = this.processPrintTemplate(this.form, printTemplate, /(\[(\w+)])/g, function (item) {
        if (item.type === 'enum') {
          var enumID = item.value;
          return item.enumValues.filter(function (enumObj) {
            return enumObj.id === enumID;
          })[0].name;
        } else {
          return item.value;
        }
      });
      printTemplate = this.processPrintTemplate(this.form, printTemplate, /(\[label=(\w+)])/g, function (item) {
        return item.name;
      });
      return $sce.trustAsHtml(printTemplate);
    }
  };

  return PrintTemplate;
});
