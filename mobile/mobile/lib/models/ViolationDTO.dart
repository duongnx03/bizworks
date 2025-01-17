import 'package:mobile/models/EmployeeResponseDTO.dart';
import 'package:mobile/models/ViolationTypeDTO.dart';

class ViolationDTO {
  final int id;
  final EmployeeResponseDTO employee;
  final ViolationTypeDTO violationType;
  final DateTime violationDate;
  final String description;
  final String status;
  final DateTime createdAt;
  final DateTime updatedAt;

  ViolationDTO({
    required this.id,
    required this.employee,
    required this.violationType,
    required this.violationDate,
    required this.description,
    required this.status,
    required this.createdAt,
    required this.updatedAt,
  });

  factory ViolationDTO.fromJson(Map<String, dynamic> json) {
    print('Processing ViolationDTO: $json'); // Kiểm tra giá trị JSON
    return ViolationDTO(
      id: json['id'], // Phải là int
      employee: EmployeeResponseDTO.fromJson(json['employee']),
      violationType: ViolationTypeDTO.fromJson(json['violationType']),
      violationDate: DateTime.parse(json['violationDate']),
      description: json['description'],
      status: json['status'],
      createdAt: DateTime.parse(json['createdAt']),
      updatedAt: DateTime.parse(json['updatedAt']),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'employee': employee.toJson(),
      'violationType': violationType.toJson(),
      'violationDate': violationDate.toIso8601String(),
      'description': description,
      'status': status,
      'createdAt': createdAt.toIso8601String(),
      'updatedAt': updatedAt.toIso8601String(),
    };
  }
}
