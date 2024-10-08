package com.bezkoder.spring.files.upload.controller;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import com.bezkoder.spring.files.upload.message.ResponseMessage;
import com.bezkoder.spring.files.upload.model.FileInfo;
import com.bezkoder.spring.files.upload.service.FilesStorageService;

@Controller
@CrossOrigin("http://localhost:8081/api")
@RequestMapping("/api")
public class FilesController {


  private static final Logger logger = LoggerFactory.getLogger(FilesController.class);

  
  @Autowired
  FilesStorageService storageService;

  @PostMapping("/upload")
  public ResponseEntity<ResponseMessage> uploadFile(@RequestParam("file") MultipartFile file) {
    String message = "";
    try {
      
      logger.info("Filename: {} ContentType:{}.", file.getName(), file.getContentType());
      
      validateMultipartFile(file);
      
      storageService.save(file);
      
      message = "Uploaded the file successfully: " + file.getOriginalFilename();
      return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
    } catch (Exception e) {
      message = "Could not upload the file: " + file.getOriginalFilename() + ". Error: " + e.getMessage();
      return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
    }
  }
  
  @PostMapping("/uploadMore")
  public ResponseEntity<ResponseMessage> uploadMoreFile(@RequestParam("file") MultipartFile[] files) {
    
    logger.info("uploadMoreFile starts.");
    
    StringBuffer message = new StringBuffer();
    MultipartFile currentMultipartFile = null;
    try {
      
      for (MultipartFile multipartFile : files) {
      
          logger.info("Filename: {} ContentType:{}.", multipartFile.getName(), multipartFile.getContentType());
          
          validateMultipartFile(multipartFile);
          
          currentMultipartFile = multipartFile;
          storageService.save(multipartFile); 
          message.append("Uploaded the file successfully: " + multipartFile.getOriginalFilename()).append(";");
      }
      
      return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message.toString()));
    } catch (Exception e) {
      message.append("Could not upload the file: " + (currentMultipartFile != null ? currentMultipartFile.getOriginalFilename() : "-") + ". Error: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message.toString()));
    }
  }
  
  /**
   * Validation: Only accept PNG and JPG formats.
   * 
   * @param file Given MultipartFile.
   * 
   * @throws Exception An exception is thrown if it is not a jpg or png file.
   */
  private void validateMultipartFile(final MultipartFile file) throws Exception {
    
    logger.debug("Filename: {} ContentType:{}.", file.getName(), file.getContentType());
    
    if (!("image/png".equals(file.getContentType()) || "image/jpg".equals(file.getContentType()))) {
    	throw new Exception("This file's content type is not png or jpg.");
    }
    
  }
  
  @GetMapping("/files")
  public ResponseEntity<List<FileInfo>> getListFiles() {
  
    logger.info("getListFiles starts.");
  
    List<FileInfo> fileInfos = storageService.loadAll().map(path -> {
      String filename = path.getFileName().toString();
      String url = MvcUriComponentsBuilder
          .fromMethodName(FilesController.class, "getFile", path.getFileName().toString()).build().toString();

      return new FileInfo(filename, url);
    }).collect(Collectors.toList());

    return ResponseEntity.status(HttpStatus.OK).body(fileInfos);
  }

  @GetMapping("/files/{filename:.+}")
  public ResponseEntity<Resource> getFile(@PathVariable String filename) {
    Resource file = storageService.load(filename);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"").body(file);
  }

  @DeleteMapping("/files/{filename:.+}")
  public ResponseEntity<ResponseMessage> deleteFile(@PathVariable String filename) {
    String message = "";
    
    try {
      boolean existed = storageService.delete(filename);
      
      if (existed) {
        message = "Delete the file successfully: " + filename;
        return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
      }
      
      message = "The file does not exist!";
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseMessage(message));
    } catch (Exception e) {
      message = "Could not delete the file: " + filename + ". Error: " + e.getMessage();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseMessage(message));
    }
  }
}
