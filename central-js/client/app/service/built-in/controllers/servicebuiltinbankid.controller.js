angular.module('app').controller('ServiceBuiltInBankIDController', function(
  $sce,
  $state,
  $stateParams,
  $scope,
  $timeout,
  $location,
  $window,
  $rootScope,
  FormDataFactory,
  ActivitiService,
  ValidationService,
  oService,
  oServiceData,
  BankIDAccount,
  activitiForm,
  AdminService,
  PlacesService,
  uiUploader,
  FieldAttributesService,
  MarkersFactory,
  service,
  FieldMotionService,
  regions) {

  'use strict';

  var currentState = $state.$current;

  $scope.oServiceData = oServiceData;
  $scope.account = BankIDAccount; // FIXME потенційний хардкод
  $scope.activitiForm = activitiForm;

  $scope.data = $scope.data || {};

  $scope.data.region = currentState.data.region;
  $scope.data.city = currentState.data.city;
  $scope.data.id = currentState.data.id;

  $scope.setFormScope = function(scope){
    this.formScope = scope;
  };

  var initializeFormData = function (){
    $scope.data.formData = new FormDataFactory();
    $scope.data.formData.initialize($scope.activitiForm);
    $scope.data.formData.setBankIDAccount(BankIDAccount);
    $scope.data.formData.uploadScansFromBankID(oServiceData);
  };


  if ( !$scope.data.formData ) {
    initializeFormData();
  }


  // console.log('data.formData.params = ', JSON.stringify($scope.data.formData.params, null, '  '));

  $scope.markers = ValidationService.getValidationMarkers();
  var aID_FieldPhoneUA = $scope.markers.validate.PhoneUA.aField_ID;

  $scope.referent = false;
  angular.forEach($scope.activitiForm.formProperties, function(field) {

    var sFieldName = field.name || '';

    // 'Як працює послуга; посилання на інструкцію' буде розбито на частини по ';'
    var aNameParts = sFieldName.split(';');
    var sFieldNotes = aNameParts[0].trim();

    field.sFieldLabel = sFieldNotes;

    sFieldNotes = null;

    if (aNameParts.length > 1) {
      sFieldNotes = aNameParts[1].trim();
      if (sFieldNotes === '') {
        sFieldNotes = null;
      }
    }
    field.sFieldNotes = sFieldNotes;

    // перетворити input на поле вводу телефону, контрольоване директивою form/directives/tel.js:
    if (_.indexOf(aID_FieldPhoneUA, field.id) !== -1) {
      field.type = 'tel';
      field.sFieldType = 'tel';
    }
    if (field.type === 'markers' && $.trim(field.value)) {
      var sourceObj = null;
      try {
        sourceObj = JSON.parse(field.value);
      } catch (ex) {
        console.log('markers attribute ' + field.name + ' contain bad formatted json\n' + ex.name + ', ' + ex.message + '\nfield.value: ' + field.value);
      }
      if (sourceObj !== null) {
        _.merge(MarkersFactory.getMarkers(), sourceObj, function(destVal, sourceVal) {
          if (_.isArray(sourceVal)) {
            return sourceVal;
          }
        });
      }
    }
  });

  //save values for each property
  $scope.persistValues = JSON.parse(JSON.stringify($scope.data.formData.params));
  $scope.getSignFieldID = function(){
    return data.formData.getSignField().id;
  };

  $scope.isSignNeeded = $scope.data.formData.isSignNeeded();
  $scope.sign = {checked : false };

  $scope.signForm = function () {
    if($scope.data.formData.isSignNeeded){
      ActivitiService.saveForm(oService, oServiceData,
        'some business key 111',
        'process name here', $scope.activitiForm, $scope.data.formData)
        .then(function (result) {
          var signPath = ActivitiService.getSignFormPath(oServiceData, result.formID, oService);
          $window.location.href = $location.protocol() + '://' + $location.host() + ':' + $location.port() + signPath;
          //$window.location.href = $location.absUrl()
          //  + '?formID=' + result.formID
          //  + '&signedFileID=' + 1122333;
        })
    } else {
      $window.alert('No sign is needed');
    }
  };

  $scope.processForm = function (form) {
    $scope.isSending = true;

    if (!$scope.validateForm(form)) {
      $scope.isSending = false;
      return false;
    }

    if ($scope.sign.checked) {
      $scope.signForm();
    } else {
      $scope.submitForm(form);
    }
  };

  $scope.validateForm = function(form) {
    var bValid = true;
    ValidationService.validateByMarkers(form, null, true);
    return form.$valid && bValid;
  };

  $scope.submitForm = function(form) {
    if(form){
      form.$setSubmitted();
    }

    ActivitiService
      .submitForm(oService, oServiceData, $scope.data.formData)
      .then(function(result) {
        $scope.isSending = false;

        var state = $state.$current;

        var submitted = $state.get(state.name + '.submitted');
        if (!result.id) {
          // console.log(result);
          return;
        }
        //TODO: Fix Alhoritm Luna
        var nCRC = ValidationService.getLunaValue(result.id);

        submitted.data.id = result.id + nCRC; //11111111
        submitted.data.formData = $scope.data.formData;

        $scope.isSending = false;

        $scope.$root.data = $scope.data;

        return $state.go(submitted, angular.extend($stateParams, {formID: null, signedFileID : null}));
      });
  };

  $scope.cantSubmit = function(form) {
    return $scope.isSending
      || ($scope.isUploading && !form.$valid)
      || ($scope.isSignNeeded && !$scope.sign.checked);
  };

  $scope.bSending = function(form) {
    return $scope.isSending;
  };

  $scope.isUploading = false;
  $scope.isSending = false;

  function getFieldProps(property) {
    if ($scope.referent && property.id.startsWith('bankId')){
      return {
        mentionedInWritable: true,
        fieldES: 1, //EDITABLE
        ES: FieldAttributesService.EditableStatus
      }
    }
    return {
      mentionedInWritable: FieldMotionService.FieldMentioned.inWritable(property.id),
      fieldES: FieldAttributesService.editableStatusFor(property.id),
      ES: FieldAttributesService.EditableStatus
    };
  }

  $scope.onReferent = function (){
    if ($scope.referent){

      angular.forEach($scope.activitiForm.formProperties, function (field){
        if (field.id.startsWith('bankId')){
          $scope.data.formData.params[field.id].value="";
        }
      });

      if ($scope.data.formData.params['bankId_scan_passport']){
        $scope.data.formData.params['bankId_scan_passport'].upload = true;
        $scope.data.formData.params['bankId_scan_passport'].scan = null;
      }

      $scope.data.formData.initialize($scope.activitiForm);

    } else {
      initializeFormData();
    }
  };

  $scope.showFormField = function(property) {
    var p = getFieldProps(property);
    if ($scope.referent && property.id.startsWith('bankId')){
      return true;
    }
    if (p.mentionedInWritable)
      return FieldMotionService.isFieldWritable(property.id, $scope.data.formData.params);

    return (
      !$scope.data.formData.fields[property.id]
      && property.type !== 'invisible'
      && property.type !== 'markers'
      && p.fieldES === p.ES.NOT_SET ) || p.fieldES === p.ES.EDITABLE;
  };

  $scope.renderAsLabel = function(property) {
    if ($scope.referent && property.id.startsWith('bankId')){
      return false;
    }
    var p = getFieldProps(property);
    if (p.mentionedInWritable)
      return !FieldMotionService.isFieldWritable(property.id, $scope.data.formData.params);
    //property.type !== 'file'
    return (
      $scope.data.formData.fields[property.id] && p.fieldES === p.ES.NOT_SET
      ) || p.fieldES === p.ES.READ_ONLY ;
  };

  $scope.isFieldVisible = function(property) {
    return property.id !== 'processName' && (FieldMotionService.FieldMentioned.inShow(property.id) ?
      FieldMotionService.isFieldVisible(property.id, $scope.data.formData.params) : true);
  };

  $scope.isFieldRequired = function(property) {
    console.log('[isFieldRequired]: property.id=' + property.id);
    if ($scope.referent && property.id == 'bankId_scan_passport'){
      return true;
    }
    console.log('[isFieldRequired]: property.id=' + property.id + ', FieldMotionService.FieldMentioned.inRequired(property.id)=' + FieldMotionService.FieldMentioned.inRequired(property.id));
    var b=FieldMotionService.FieldMentioned.inRequired(property.id) ?
      FieldMotionService.isFieldRequired(property.id, $scope.data.formData.params) : property.required;
    console.log('[isFieldRequired]: property.id=' + property.id + ', b=' + b);
    console.log('[isFieldRequired]: property.id=' + property.id + ', property.required=' + property.required);
    return b;
  };

  $scope.$watch('data.formData.params', watchToSetDefaultValues, true);
  function watchToSetDefaultValues() {
    var calcFields = FieldMotionService.getCalcFieldsIds();
    var pars = $scope.data.formData.params;
    calcFields.forEach(function(key) {
      if (_.has(pars, key)) {
        var data = FieldMotionService.calcFieldValue(key, pars);
        if (data.value && data.differentTriggered) pars[key].value = data.value;
      }
    });
  }

  $scope.htmldecode = function(encodedhtml)
  {
    var map = {
      '&amp;'     :   '&',
      '&gt;'      :   '>',
      '&lt;'      :   '<',
      '&quot;'    :   '"',
      '&#39;'     :   "'"
    };

    var result = angular.copy(encodedhtml);
    angular.forEach(map, function(value, key)
    {
      while(result.indexOf(key) > -1)
        result = result.replace(key, value);
    });

    return result;
  };

  $scope.getHtml = function(html) {
    return $sce.trustAsHtml(html);
  };

  if($scope.data.formData.isAlreadySigned() && $stateParams.signedFileID){
    var state = $state.$current;
    //TODO remove ugly hack for not calling submit after submit
    if(!state.name.endsWith('submitted')){
      $scope.submitForm();
    }
  }

});
